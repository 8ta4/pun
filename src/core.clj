(ns core
  (:require
   [cheshire.core :refer [parse-string]]
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

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
