(ns core
  (:require
   [cheshire.core :refer [parse-string]]
   [clojure.java.io :as io]
   [clojure.string :as string])
  (:import
   (java.io BufferedReader InputStreamReader)
   (java.util.zip GZIPInputStream)))

(def wiktextract-data-path
  (str (System/getProperty "user.home") "/.cache/pun/raw-wiktextract-data.jsonl.gz"))

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

(defn save
  []
  (->> (extract)
       distinct
       sort
       (string/join "\n")
       (spit "vocabulary.txt")))

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
