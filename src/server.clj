(ns server
  (:require
   [clojure.edn :as edn]
   [core :refer [ipa-path normalized-path]]))

(def phrase-scores
  (edn/read-string (slurp normalized-path)))

(def phrase-ipas
  (edn/read-string (slurp ipa-path)))