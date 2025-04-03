(ns core
  (:require
   [clojure.java.io :as io]
   [cheshire.core :refer [parse-string]]))

(def wiktextract-data-path
  (str (System/getProperty "user.home") "/.cache/pun/raw-wiktextract-data.jsonl.gz"))

(defn load-wiktextract-data
  []
  (->> wiktextract-data-path
       io/input-stream
       java.util.zip.GZIPInputStream.
       java.io.InputStreamReader.
       java.io.BufferedReader.
       line-seq
       (map #(parse-string % keyword))
       (filter (comp (partial = "English") :lang))))

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
