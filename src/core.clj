(ns core)

(def wiktextract-data-path
  (str (System/getProperty "user.home") "/.cache/pun/raw-wiktextract-data.jsonl.gz"))

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
