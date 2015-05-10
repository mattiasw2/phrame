(ns phrame.storage
  (:refer-clojure :exclude [swap!])
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
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

             :picasa-album {:url [{:type :string
                                   :required true}]
                            :title [{:type :string
                                     :required true}]
                            :id [{:type :long
                                  :required true
                                  :unique :value}]}

             :frame {:key [{:type :string
                            :unique :value}]
                     :name [{:type :string}]
                     :token [{:type :string}]
                     :owner [{:type :ref
                              :ref {:ns :user}}]
                     :feed [{:type :ref
                             :ref {:ns :picasa-album}}]}})

(defn connect [& {:keys [init? install-schema?]}]
  (adi/connect! (:db-url config/config)
                schema
                init?
                install-schema?))

(defn migrate []
  (let [ds (connect :init? true :install-schema? true)]
    (adi/insert! ds (vec (map (fn [user]
                                {:user (set/rename-keys user {:refresh-token :google-refresh-token})})
                              (vals (:users @data)))))
    (adi/insert! ds (vec (map (fn [album]
                                {:picasa-album album})
                              (apply set/union (map (comp hash-set :album) (vals (:phrames @data)))))))
    (adi/insert! ds (vec (map (fn [[id phrame]]
                                (println "id" id "album-id" (:id (:album phrame)))
                                {:frame {:owner [[:user/email (:owner phrame)]]
                                         :feed {:id (:id (:album phrame))}
                                         :key id
                                         :token (:token phrame)}})
                              (:phrames @data))))))
