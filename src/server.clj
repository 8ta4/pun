(ns server
  (:require
   [clojure.edn :as edn]
   [core :refer [ipa-path normalized-path]]
   [libpython-clj2.python :refer [from-import]]
   [libpython-clj2.require]))

(from-import Levenshtein distance)

(def phrase-scores
  (edn/read-string (slurp normalized-path)))

(def phrase-ipas
  (edn/read-string (slurp ipa-path)))

(def has-space?
  (partial re-find #" "))

(def recognizability-threshold
  50)

(def recognizable-words
  (->> phrase-scores
       (remove (comp has-space? first))
       (filter (comp (partial < recognizability-threshold) second))
       (map first)))

(defn calculate-normalized-distance
  [original replacement]
  (/ (distance original replacement) (count original)))