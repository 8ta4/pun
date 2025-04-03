(ns core)

(def cache-path
  (str (System/getProperty "user.home") "/.cache/pun"))

(def url
  "https://kaikki.org/dictionary/raw-wiktextract-data.jsonl.gz")

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
