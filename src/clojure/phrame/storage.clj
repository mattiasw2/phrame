(ns phrame.storage
  (:refer-clojure :exclude [swap!])
  (:require [clojure.java.io :as io]
            [alandipert.enduro :as enduro]
            [adi.core :as adi]
            [phrame.config :as config]))

;; Enduro based

(def directory (System/getProperty "user.home"))

(def data (enduro/file-atom {}
                            (io/file directory ".phrame.edn")
                            :pending-dir directory))

(def swap! (partial enduro/swap! data))

;; Datomic based

(def schema {:user {:email [{:type :string
                             :unique :value}]
                    :google-refresh-token [{:type :string}]
                    :password [{:type :string}]}

             :phrame {:key [{:type :string
                             :unique :value}]
                      :name [{:type :string}]
                      :owner [{:type :ref
                               :ref {:ns :user}}]
                      :feed [{:type :ref
                              :ref {:ns :picasa-album}}]}

             :picasa-album {:url [{:type :string
                                   :required true}]
                            :name [{:type :string
                                    :required true}]
                            :id [{:type :long
                                  :required true}]}})

(defn connect [& {:keys [init? install-schema?]}]
  (adi/connect! (:db-url config/config)
                schema
                init?
                install-schema?))
