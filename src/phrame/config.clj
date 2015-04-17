(ns phrame.config
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def config (edn/read-string (slurp (io/resource "config.edn"))))
