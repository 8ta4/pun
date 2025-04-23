(ns core
  (:require
   [clojure.java.io :as io]))

(def cache-path
  (io/file (System/getProperty "user.home") ".cache/pun"))

(def normalized-path
  (io/file cache-path "normalized.edn"))

(def ipa-path
  (io/file cache-path "ipa.edn"))