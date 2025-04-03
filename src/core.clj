(ns core)

(def cache-path
  (str (System/getProperty "user.home") "/.cache/pun"))

(defn -main
  "The main entry point for the application"
  [& args]
  (println "Hello, World!"))
