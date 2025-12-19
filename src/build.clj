(ns build
  (:require
   [buddy.core.codecs :refer [bytes->hex]]
   [buddy.core.hash :refer [sha256]]
   [cheshire.core :refer [parse-string]]
   [clj-http.client :refer [post]]
   [clojure.edn :as edn]
   [clojure.java.io :refer [file input-stream make-parents]]
   [clojure.set :refer [difference]]
   [clojure.string :as string :refer [join split-lines]]
   [com.rpl.specter :refer [ALL select*]]
   [core :refer [get-ipa]]
   [incanter.stats :refer [mean]])
  (:import
   (java.io BufferedReader InputStreamReader)
   (java.util.zip GZIPInputStream)))

(def data-path
  "resources")

(def wiktextract-data-path
  (file data-path "raw-wiktextract-data.jsonl.gz"))

(defn load-wiktextract
  []
  (->> wiktextract-data-path
       input-stream
       GZIPInputStream.
       InputStreamReader.
       BufferedReader.
       line-seq
       (map #(parse-string % keyword))
       (filter (comp (partial = "English") :lang))
       (map :word)))

(def vocabulary-path
  (file data-path "vocabulary.txt"))

(defn spit-make-parents
  "Like clojure.core/spit, but creates parent directories."
  [f content & options]
  (make-parents f)
  (apply spit f content options))

(defn save-vocabulary
  []
  (println "Generating vocabulary...")
  (->> (load-wiktextract)
       distinct
       sort
       (join "\n")
       (spit-make-parents vocabulary-path)))

(def key-path
  (file (System/getProperty "user.home") ".config/pun/key"))

(defn get-anthropic-key
  []
  (slurp key-path))

(def anthropic-version "2023-06-01")

(defn post-batch
  [requests]
  (post "https://api.anthropic.com/v1/messages/batches"
        {:headers {:x-api-key (get-anthropic-key)
                   :anthropic-version anthropic-version
                   :content-type "application/json"}
         :body (cheshire.core/generate-string {:requests requests})
         :as :json}))

(def system
  (slurp "system.txt"))

(def generate-prefill
  (comp (partial str "{\n") pr-str))

(def generate-id
  (comp bytes->hex sha256))

(defn create-request
  [phrase]
; The custom_id must match the pattern '^[a-zA-Z0-9_-]{1,64}$'. Raw phrases might not.
  {:custom_id (generate-id phrase)
   :params {:model "claude-3-7-sonnet-20250219"
            :max_tokens 32
            :temperature 0
            :system system
            :messages [{:role "user" :content (str "Phrases:\n" phrase "\ntouchstone")}
                       {:role "assistant" :content (generate-prefill phrase)}]}})

(defn create-requests
  [phrases]
  (map create-request phrases))

(defn list-batches
  []
  (client/get "https://api.anthropic.com/v1/messages/batches"
              {:headers {:x-api-key (get-anthropic-key)
                         :anthropic-version anthropic-version}
               :as :json}))

(defn fetch-batch-data
  []
  (-> (list-batches)
      :body
      :data))

(defn get-batch-results
  [results-url]
  (client/get results-url
              {:headers {:x-api-key (get-anthropic-key)
                         :anthropic-version anthropic-version}}))

(def results-path
  (file data-path "results"))

(defn save-batch-results
  [batch]
  (->> batch
       :results_url
       get-batch-results
       :body
       (spit-make-parents (file results-path (str (:id batch) ".jsonl")))))

(defn save-results
  []
  (println "Saving results...")
  (dorun (map save-batch-results (fetch-batch-data))))

(defn load-results
  []
  (->> results-path
       .listFiles
       (mapcat (comp split-lines slurp))
       (map #(parse-string % keyword))
       (filter (comp (partial = "succeeded") :type :result))))

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
  (file data-path "raw.edn"))

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
  10000)

(defn load-vocabulary
  []
  (into (sorted-set) (split-lines (slurp vocabulary-path))))

(defn get-id-phrase-map
  []
  (into {} (map (juxt generate-id identity) (load-vocabulary))))

(defn load-successful-phrases
  []
  (set (map (comp (get-id-phrase-map) :custom_id) (load-results))))

(defn get-remaining-phrases
  []
  (difference (load-vocabulary) (load-successful-phrases)))

(defn latest-batch-incomplete?
  []
  (-> (fetch-batch-data)
; Most recently created batches are returned first.
; https://docs.anthropic.com/en/api/listing-message-batches
      first
; URL to a .jsonl file containing the results of the Message Batch requests. Specified only once processing ends.
; https://docs.anthropic.com/en/api/listing-message-batches
      :results_url
      nil?))

(defn await-batch
  []
  (while (latest-batch-incomplete?)
    (Thread/sleep sleep-duration)))

(defn empty-sequential?
  [x]
  (and (sequential? x) (empty? x)))

(defn wait-and-send-batch
  [batch]
  (when-not (empty-sequential? (fetch-batch-data))
    (await-batch))
  (println "Sending batch...")
  (post-batch (create-requests batch)))

(defn submit-batches
  []
  (println "Submitting batches...")
  (->> (get-remaining-phrases)
       (partition-all batch-size)
       (map wait-and-send-batch)
       dorun))

(defn try-parse-edn
  [s]
  (try (edn/read-string s)
       (catch RuntimeException _ nil)))

(defn load-and-parse-scores
  []
  (let [id-phrase-map (get-id-phrase-map)]
    (->> raw-path
         slurp
         edn/read-string
         (map (fn [[k v]]
                (-> k
                    id-phrase-map
                    generate-prefill
                    (str " " v)
                    try-parse-edn)))
         (remove nil?))))

(def benchmark-word
  "touchstone")

(def compute-mean
  (comp (partial mean)
        (partial select* [ALL benchmark-word])))

(defn normalize-score-entry
  [mean-benchmark-score score-entry]
  (let [benchmark-score (get score-entry benchmark-word)
        target-key (-> score-entry
                       keys
                       set
                       (disj benchmark-word)
                       first
                       (or benchmark-word))
        target-score (get score-entry target-key)]
    {target-key (if (<= target-score benchmark-score)
                  (/ (* target-score mean-benchmark-score) benchmark-score)
                  (- 100.0
                     (/ (* (- 100.0 target-score) (- 100.0 mean-benchmark-score))
                        (- 100.0 benchmark-score))))}))

(def normalized-path
  (file data-path "normalized.edn"))

(defn save-normalized
  []
  (println "Starting normalization process...")
  (let [scores (load-and-parse-scores)]
    (->> scores
         (map (partial normalize-score-entry (compute-mean scores)))
         (reduce merge)
         (spit-make-parents normalized-path))))

(def ipa-path
  (file data-path "ipa.edn"))

(defn generate-ipa-map
  []
  (into {} (map (juxt identity get-ipa) (load-vocabulary))))

(defn save-ipa
  []
  (spit-make-parents ipa-path (generate-ipa-map)))

(defn -main
  [& args]
  (case (first args)
    "vocabulary" (save-vocabulary)
    "batches" (submit-batches)
    "results" (save-results)
    "raw" (save-raw)
    "normalized" (save-normalized)
    "ipa" (save-ipa)
    (println "Invalid command.")))