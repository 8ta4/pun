(ns core
  (:require
   [clojure.java.io :refer [file]]
   [libpython-clj2.python :refer [$a]]
   [libpython-clj2.require :refer [require-python]]))

(require-python 'epitran)

(def cache-path
  (file (System/getProperty "user.home") ".cache/pun"))

(def normalized-path
  (file cache-path "normalized.edn"))

(def ipa-path
  (file cache-path "ipa.edn"))

(def model
  (epitran/Epitran "eng-Latn"))

(defn get-ipa
  [s]
  ($a model transliterate s))