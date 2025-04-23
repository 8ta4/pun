(ns server
  (:require
   [clojure.edn :as edn]
   [core :refer [ipa-path normalized-path]]))

(def phrase-scores
  (edn/read-string (slurp normalized-path)))

(def phrase-ipas
  (edn/read-string (slurp ipa-path)))

(def has-space?
  (partial re-find #" "))

(def word-scores
  (into {} (remove (comp has-space? first) phrase-scores)))