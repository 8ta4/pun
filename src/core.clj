(ns core
  (:require [clojure.java.shell :refer [sh]]))

(def cache-path
  (str (System/getProperty "user.home") "/.cache/pun"))

(def url
  "https://kaikki.org/dictionary/raw-wiktextract-data.jsonl.gz")

(defn download
  []
  (sh "wget" "-Nc" "-P" cache-path url))

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
