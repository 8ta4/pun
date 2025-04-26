(ns server
  (:require
   [clojure.edn :as edn]
   [clojure.math.combinatorics :refer [cartesian-product]]
   [clojure.string :as string]
   [core :refer [get-ipa ipa-path normalized-path]]
   [libpython-clj2.python :refer [from-import]]
   [libpython-clj2.require]
   [clojure.set :as set]))

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
                       (partial set/intersection (set similar-words))
                       set
                       #(string/split % #" ")))
         (cartesian-product similar-words)
         (mapcat (fn [[original-word phrase]]
                   (if (and (re-find (create-boundary-regex original-word) phrase)
                            (not= original-word substitute-word))
                     [(string/replace phrase (create-boundary-regex original-word) substitute-word)]
                     [])))
         distinct)))