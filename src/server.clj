(ns server
  (:require
   [clojure.edn :as edn]
   [core :refer [get-ipa ipa-path normalized-path]]
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