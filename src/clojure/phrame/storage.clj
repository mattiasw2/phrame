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

(defmacro with-connection [& body]
  `(binding [*conn* (connect)]
     ~@body))

(defn get-entity
  ([db-id]
   (datomic/load-entity (datomic/db (conn))
                        db-id))
  ([key value]
   (datomic/load-entity (datomic/db (conn))
                        [key value])))

(defn get-frames []
  (map #(dissoc % :token)
       (datomic/select-entities '[:find [?e]
                                  :where [?e :frame/key ?key]]
                                (datomic/db (conn)))))

(defn get-frame [key]
  (get-entity :frame/key key))

(defn upsert-frame [key updates]
  (datomic/assert! (conn)
                   (merge {:<type> :frame
                           :key key}
                          (get-frame key)
                          updates))
  (get-frame key))

(defn get-users []
  (datomic/select-entities '[:find [(pull ?e [:db/id :user/email])]
                             :where [?e :user/email ?email]]
                           (datomic/db (conn))))

(defn get-user [email]
  (with-connection
    (let [user (get-entity :user/email email)]
      (into user
            {:albums (mapv #(datomic/load-entity (datomic/db *conn*) (:db/id %)) (:albums user))}))))

(defn upsert-user [email updates]
  (datomic/assert! (conn)
                   (merge {:<type> :user
                           :email email}
                          (get-user email)
                          updates))
  (get-user email))

(defn get-album [picasa-id]
  (with-connection
    (when-let [album (get-entity :picasa-album/picasa-id picasa-id)]
      (into album
            {:images (mapv #(datomic/load-entity (datomic/db *conn*) (:db/id %)) (:images album))}))))

(defn upsert-album [picasa-id updates]
  (datomic/assert! (conn)
                   (merge {:<type> :picasa-album}
                          (dissoc (or (get-album picasa-id) {}) :images)
                          updates))
  (get-album picasa-id))

(defn delete-album [picasa-id]
  @(d/transact (:connection (conn))
               [[:db.fn/retractEntity [:picasa-album/picasa-id picasa-id]]]))

(defn get-image [picasa-id]
  (get-entity :picasa-image/picasa-id picasa-id))

(defn upsert-image [picasa-id updates]
  (datomic/assert! (conn)
                   (merge {:<type> :picasa-image}
                          (get-image picasa-id)
                          updates))
  (get-image picasa-id))

(defn delete-image [picasa-id]
  @(d/transact (:connection (conn))
               [[:db.fn/retractEntity [:picasa-image/picasa-id picasa-id]]]))
