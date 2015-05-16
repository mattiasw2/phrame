(ns phrame.storage
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [datomic.api :as d]
            [lambdawerk.datomic :as datomic]
            [lambdawerk.datomic-schema :as datomic-schema]
            [phrame.config :as config]))

(defn connect [& {:keys [init? install-schema?]}]
  (let [url (:db-url config/config)]
    (when init?
      (d/create-database url))
    (datomic/connect url
                     (io/resource "db-schema.edn")
                     :install-schema? install-schema?)))

(def ^:dynamic *conn* nil)

(defn conn []
  (or *conn*
      (connect)))

(defn get-entity [key value]
  (first (datomic/select-entities '[:find [?e]
                                    :in $ ?key ?value
                                    :where [?e ?key ?value]]
                                  (datomic/db (conn))
                                  key value)))

;; 

(defn get-frames []
  (map #(dissoc (apply merge (vals %)) :token)
       (datomic/select-entities '[:find [?e]
                                  :where [?e :frame/key ?key]]
                                (datomic/db (conn)))))

(defn get-frame [key]
  (get-entity :frame/key key))

(defn update-frame [key updates]
  (get-frame key))

(defn get-users []
  (datomic/select-entities '[:find [?e]
                             :where [?e :user/email ?email]]
                           (datomic/db (conn))))

(defn get-user [email]
  (get-entity :user/email email))

(defn update-user [email updates]
  (get-user email))

