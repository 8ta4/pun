(ns core
  (:require
   [libpython-clj2.python :refer [$a]]
   [libpython-clj2.require :refer [require-python]]))

(require-python 'epitran)

(def model
  (epitran/Epitran "eng-Latn"))

(defn get-ipa
  [s]
  ($a model transliterate s))