(ns server
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :refer [resource]]
   [clojure.math.combinatorics :refer [cartesian-product]]
   [clojure.set :refer [intersection]]
   [clojure.string :as string :refer [split]]
   [core :refer [get-ipa normalized-edn ipa-edn]]
   [libpython-clj2.python :refer [from-import]]
   [libpython-clj2.require]
   [mount.core :refer [defstate start]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [ring.util.response :refer [response]]))

(from-import Levenshtein distance)

(def phrase-scores
  (edn/read-string (slurp (resource normalized-edn))))

(def phrase-ipas
  (edn/read-string (slurp (resource ipa-edn))))

(def has-space?
  (partial re-find #" "))

(def recognizability-threshold
  50)

(def recognizable-phrases
  (map first (filter (comp (partial < recognizability-threshold) second) phrase-scores)))

(def recognizable-words
  (remove has-space? recognizable-phrases))

(defn calculate-normalized-distance
  [original replacement]
  (/ (distance original replacement) (count original)))

(def similarity-threshold
  0.5)

(defn find-similar-words
  [word]
  (->> recognizable-words
       (select-keys phrase-ipas)
       (filter (comp (partial > similarity-threshold)
                     (partial calculate-normalized-distance (get-ipa word))
                     last))
       (map first)))

(def recognizable-multi-word-phrases
  (filter has-space? recognizable-phrases))

(defn create-boundary-regex
  [word]
  (re-pattern (str "\\b" word "\\b")))

(defn generate-puns
  [substitute-word]
  (let [similar-words (find-similar-words substitute-word)]
    (->> recognizable-multi-word-phrases
; Efficiently generates puns by filtering phrases prior to the cartesian product.
; This drastically reduces intermediate computations and allocations.
; Observed ~5558ms -> ~916ms elapsed time for `(time (doall (generate-puns "pun")))`
         (remove (comp empty?
                       (partial intersection (set similar-words))
                       set
                       #(split % #" ")))
         (cartesian-product similar-words)
         (mapcat (fn [[original-word phrase]]
                   (if (and (re-find (create-boundary-regex original-word) phrase)
                            (not= original-word substitute-word))
                     [(string/replace phrase (create-boundary-regex original-word) substitute-word)]
                     [])))
         distinct)))

(def handler
  (comp response
        set
        (partial mapcat generate-puns)
        :body))

(def app
  (wrap-json-response (wrap-json-body handler)))

(defstate server
  :start (run-jetty app {:join? false
                         :port 3000})
  :stop (.stop server))

(defn -main
  [& args]
  (start)
  @(promise))