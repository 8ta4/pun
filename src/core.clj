(ns core
  (:require
   [clojure.java.io :as io]
   [cheshire.core :refer [parse-string]])
  (:import
   (java.util.zip GZIPInputStream)
   (java.io InputStreamReader BufferedReader)))

(def wiktextract-data-path
  (str (System/getProperty "user.home") "/.cache/pun/raw-wiktextract-data.jsonl.gz"))

(defn load-wiktextract-data
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

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
