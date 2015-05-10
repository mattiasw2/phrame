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
                             :unique :value
                             :required true}]
                    :google-refresh-token [{:type :string}]
                    :password [{:type :string}]
                    :albums [{:type :ref
                              :ref {:ns :user}}]}

             :image {:picasa-id [{:type :long
                                  :unique :value
                                  :required true}]
                     :published [{:type :instant}]
                     :updated [{:type :instant}]
                     :title [{:type :string}]
                     :width [{:type :long}]
                     :height [{:type :long}]
                     :size [{:type :long}]
                     :url [{:type :string}]
                     :thumbnail-url [{:type :string}]}

             :picasa-album {:url [{:type :string
                                   :required true}]
                            :title [{:type :string
                                     :required true}]
                            :picasa-id [{:type :long
                                         :required true
                                         :unique :value}]
                            :updated [{:type :instant}]
                            :images [{:type :ref
                                      :ref {:ns :image}}]}

             :frame {:key [{:type :string
                            :unique :value
                            :required true}]
                     :name [{:type :string}]
                     :token [{:type :string}]
                     :owner [{:type :ref
                              :ref {:ns :user}}]
                     :feed [{:type :ref
                             :ref {:ns :picasa-album}}]}

             :impression {:frame [{:type :ref
                                   :ref {:ns :frame}
                                   :required true}]
                          :image [{:type :ref
                                    :ref {:ns :image}
                                    :required true}]
                          :when [{:type :instant
                                  :require :true}]}})

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
                                (let [owner-id (adi/select ds {:user {:email (:owner phrame)}} :return :ids :first true)
                                      feed-id (adi/select ds {:picasa-album {:id (:id (:album phrame))}} :return :ids :first true)]
                                  {:frame {:owner owner-id
                                           :feed feed-id
                                           :key id
                                           :token (:token phrame)}}))
                              (:phrames @data))))))

(def ^:dynamic *ds* nil)

(defn ds []
  (or *ds*
      (connect)))

(defn get-entity [type key]
  (apply merge (vals (adi/select (ds) {type key} :first true :ids true))))

;; 

(defn get-frames []
  (map #(dissoc (apply merge (vals %)) :token) (adi/select (ds) :frame :ids true)))

(defn get-frame [key]
  (get-entity :frame {:key key}))

(defn update-frame [key updates]
  (get-frame key))

(defn get-users []
  (map :user (adi/select (ds) :user)))

(defn get-user [email]
  (get-entity :user {:email email}))

(defn update-user [email updates]
  (get-user email))

