(ns core
  (:require
   [clojure.java.io :as io]))

(def cache-path
  (io/file (System/getProperty "user.home") ".cache/pun"))