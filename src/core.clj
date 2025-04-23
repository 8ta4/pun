(ns core
  (:require
   [clojure.java.io :as io]
   [libpython-clj2.python :refer [$a]]
   [libpython-clj2.require :refer [require-python]]))

(require-python 'epitran)

(def cache-path
  (io/file (System/getProperty "user.home") ".cache/pun"))

(def normalized-path
  (io/file cache-path "normalized.edn"))

(def ipa-path
  (io/file cache-path "ipa.edn"))

(def model
  (epitran/Epitran "eng-Latn"))

(defn get-ipa
  [s]
  ($a model transliterate s))