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

(defn load-wiktextract
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

(defn save-vocabulary
  []
  (println "Generating vocabulary...")
  (->> (load-wiktextract)
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

(defn post-batch
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
  (post-batch (create-requests phrases)))

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
       (reduce merge)
       (spit-make-parents raw-path)))

(def batch-size
; https://docs.anthropic.com/en/api/rate-limits
  100000)

(def sleep-duration
  60000)

(defn manage-workflow
  []
  (when (:results_url (get-latest-batch))
    (println "Saving results...")
    (save-latest-batch-results)
    (send-batch (take batch-size (get-remaining-phrases))))
  (Thread/sleep sleep-duration)
  (recur))

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
  [mean-benchmark-score score-entry]
  (let [benchmark-score (get score-entry benchmark-word)
        target-key (-> score-entry
                       keys
                       set
                       (disj benchmark-word)
                       first)
        target-score (get score-entry target-key)]
    {target-key (if (<= target-score benchmark-score)
                  (/ (* target-score mean-benchmark-score) benchmark-score)
                  (- 100.0
                     (/ (* (- 100.0 target-score) (- 100.0 mean-benchmark-score))
                        (- 100.0 benchmark-score))))}))

(def normalized-path
  (io/file cache-path "normalized.edn"))

(defn save-normalized
  []
  (println "Starting normalization process...")
  (let [scores (load-and-parse-scores)]
    (->> scores
         (map (partial normalize-score-entry (compute-mean scores)))
         (reduce merge)
         (spit-make-parents normalized-path))))

(defn -main
  [& args]
  (case (first args)
    "vocabulary" (save-vocabulary)
    "raw" (manage-workflow)
    "normalized" (save-normalized)
    (println "Invalid command.")))