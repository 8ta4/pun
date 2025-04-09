(ns core
  (:require
   [cheshire.core :refer [parse-string]]
   [clj-http.client :as client]
   [clj-yaml.core :as yaml]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as string]
   [incanter.stats :as stats]
   [com.rpl.specter :as s])
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

(defn spit-make-parents
  "Like clojure.core/spit, but creates parent directories."
  [f content & options]
  (io/make-parents f)
  (apply spit f content options))

(defn save
  []
  (->> (extract)
       distinct
       sort
       (string/join "\n")
       (spit-make-parents vocabulary-path)))

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

(defn generate-prefill
  [phrase]
  (str "{\n\"" phrase "\""))

(defn create-request
  [phrase]
  {:custom_id phrase
   :params {:model "claude-3-7-sonnet-20250219"
            :max_tokens 32
            :temperature 0
            :system system
            :messages [{:role "user" :content (str "Phrases:\n" phrase "\ntouchstone")}
                       {:role "assistant" :content (generate-prefill phrase)}]}})

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

(defn get-latest-batch
  []
  (-> (list-batches)
      :body
      :data
      ; "Most recently created batches are returned first."
      ; https://docs.anthropic.com/en/api/listing-message-batches
      first))

(defn get-batch-results
  [results-url]
  (client/get results-url
              {:headers {:x-api-key (get-anthropic-key)
                         :anthropic-version anthropic-version}}))

(def results-path
  (io/file cache-path "results"))

(defn save-latest-batch-results
  []
  (let [latest-batch (get-latest-batch)]
    (->> latest-batch
         :results_url
         get-batch-results
         :body
         (spit-make-parents (io/file results-path (str (:id latest-batch) ".jsonl"))))))

(defn load-results
  []
  (->> results-path
       .listFiles
       (mapcat (comp string/split-lines slurp))
       (map #(parse-string % keyword))
       (filter (comp (partial = "succeeded") :type :result))))

(defn load-successful-phrases
  []
  (set (map :custom_id (load-results))))

(defn load-vocabulary
  []
  (into (sorted-set) (string/split-lines (slurp vocabulary-path))))

(defn get-remaining-phrases
  []
  (set/difference (load-vocabulary) (load-successful-phrases)))

(defn get-result-text
  [successful-result]
  (->> successful-result
       :result
       :message
       :content
       first
       :text))

(defn build-score-entry
  [successful-result]
  {(:custom_id successful-result) (get-result-text successful-result)})

(def raw-path
  (io/file cache-path "raw.edn"))

(defn save-raw
  []
  (->> (load-results)
       (map build-score-entry)
       (apply merge)
       (spit raw-path)))

(defn load-and-parse-scores
  []
  (->> raw-path
       slurp
       edn/read-string
       (map (fn [[k v]] (edn/read-string (str (generate-prefill k) " " v))))))

(def benchmark-word
  "touchstone")

(def compute-mean
  (comp (partial stats/mean)
        (partial s/select* [s/ALL benchmark-word])))

(defn normalize-score-entry
  [benchmark-mean score-entry])

(defn normalize-scores
  []
  (let [scores (load-and-parse-scores)]
    (map (partial normalize-score-entry (compute-mean scores)) scores)))

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
