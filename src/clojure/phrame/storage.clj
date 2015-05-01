(ns phrame.storage
  (:refer-clojure :exclude [swap!])
  (:require [clojure.java.io :as io]
            [alandipert.enduro :as enduro]))

(def directory (System/getProperty "user.home"))

(def data (enduro/file-atom {}
                            (io/file directory ".phrame.edn")
                            :pending-dir directory))

(def swap! (partial enduro/swap! data))
