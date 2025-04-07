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
  (io/file (System/getProperty "user.home") ".cache/pun"))

(def wiktextract-data-path
  (io/file cache-path "raw-wiktextract-data.jsonl.gz"))

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
  (io/file cache-path "vocabulary.txt"))

(defn save
  []
  (->> (extract)
       distinct
       sort
       (string/join "\n")
       (spit vocabulary-path)))

(def config-path
  (io/file (System/getProperty "user.home") ".config/pun/config.yaml"))

(defn get-anthropic-key
  []
  (-> config-path
      slurp
      yaml/parse-string
      :key))

(def anthropic-version "2023-06-01")

(defn send-batch*
  [requests]
  (client/post "https://api.anthropic.com/v1/messages/batches"
               {:headers {:x-api-key (get-anthropic-key)
                          :anthropic-version anthropic-version
                          :content-type "application/json"}
                :body (cheshire.core/generate-string {:requests requests})
                :as :json}))

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
  (client/get (str "https://api.anthropic.com/v1/messages/batches/" batch-id)
              {:headers {:x-api-key (get-anthropic-key)
                         :anthropic-version anthropic-version}
               :as :json}))

(defn list-batches
  []
  (client/get "https://api.anthropic.com/v1/messages/batches"
              {:headers {:x-api-key (get-anthropic-key)
                         :anthropic-version anthropic-version}
               :as :json}))

(defn get-latest-results-url
  []
  (-> (list-batches)
      :body
      :data
      ; "Most recently created batches are returned first."
      ; https://docs.anthropic.com/en/api/listing-message-batches
      first
      :results_url))

(defn get-batch-results
  [results-url]
  (client/get results-url
              {:headers {:x-api-key (get-anthropic-key)
                         :anthropic-version anthropic-version}}))

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
