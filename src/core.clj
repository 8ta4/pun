(ns core
  (:require
   [cheshire.core :refer [parse-string]]
   [clj-http.client :as client]
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [clojure.string :as string])
  (:import
   (java.io BufferedReader InputStreamReader)
   (java.util.zip GZIPInputStream)))

(def cache-path
  (str (System/getProperty "user.home") "/.cache/pun/"))

(def wiktextract-data-path
  (str cache-path "raw-wiktextract-data.jsonl.gz"))

(defn extract
  []
  (->> wiktextract-data-path
       io/input-stream
       GZIPInputStream.
       InputStreamReader.
       BufferedReader.
       line-seq
       (map #(parse-string % keyword))
       (filter (comp (partial = "English") :lang))
       (map :word)))

(def vocabulary-path
  (str cache-path "vocabulary.txt"))

(defn save
  []
  (->> (extract)
       distinct
       sort
       (string/join "\n")
       (spit vocabulary-path)))

(def config-path
  (str (System/getProperty "user.home") "/.config/pun/config.yaml"))

(defn get-anthropic-key
  []
  (-> config-path
      slurp
      yaml/parse-string
      :key))

(def anthropic-version "2023-06-01")

(defn send-batch*
  [requests]
  (let [api-key (get-anthropic-key)
        url "https://api.anthropic.com/v1/messages/batches"
        headers {:x-api-key api-key
                 :anthropic-version anthropic-version
                 :content-type "application/json"}
        body {:requests requests}]
    (client/post url
                 {:headers headers
                  :body (cheshire.core/generate-string body)
                  :as :json})))

(def system
  (slurp "system.txt"))

(defn create-request
  [phrase]
  {:custom_id phrase
   :params {:model "claude-3-7-sonnet-20250219"
            :max_tokens 32
            :temperature 0
            :system system
            :messages [{:role "user" :content (str "Phrases:\n" phrase "\ntouchstone")}
                       {:role "assistant" :content (str "{\n\"" phrase "\"")}]}})

(defn create-requests
  [phrases]
  (map create-request phrases))

(defn send-batch
  [phrases]
  (send-batch* (create-requests phrases)))

(defn get-batch
  "Retrieve a message batch"
  [batch-id]
  (let [api-key (get-anthropic-key)
        url (str "https://api.anthropic.com/v1/messages/batches/" batch-id)
        headers {:x-api-key api-key
                 :anthropic-version anthropic-version}]
    (client/get url
                {:headers headers
                 :as :json})))

(defn list-batches
  []
  (let [api-key (get-anthropic-key)
        url "https://api.anthropic.com/v1/messages/batches"
        headers {:x-api-key api-key
                 :anthropic-version anthropic-version}]
    (client/get url
                {:headers headers
                 :as :json})))

(defn get-latest-batch
  []
  (-> (list-batches)
      :body
      :data
      ; "Most recently created batches are returned first."
      ; https://docs.anthropic.com/en/api/listing-message-batches
      first))

(defn latest-batch-in-progress?
  []
  (= "in_progress" (:processing_status (get-latest-batch))))

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
