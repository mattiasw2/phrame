(ns lambdawerk.datomic
  (:require [clj-time.coerce :as time-coerce]
            [datomic.api :as d]
            [slingshot.slingshot :refer [throw+]]
            [lambdawerk.datomic-schema :as datomic-schema]))

(defn convert-type [attribute-name obj]
  (when (nil? obj) ;; allow false but not nil
    (throw+ {:type ::unexpected-nil-value-for-attribute :attribute attribute-name}))
  (or (if (instance? org.joda.time.DateTime obj)
        (time-coerce/to-date obj)
        obj)))

(defn convert-keyword [type-ns attribute key]
  (keyword (str (name type-ns) "." (name attribute)) (name key)))

(defn make-entity-map [entity]
  (let [type-ns (:<type> entity)
        entity (dissoc (into entity
                             {:db/id (or (:db/id entity)
                                         (d/tempid :db.part/user))})
                       :<type>)]
    (into {} (map (fn [[key value]]
                    [(if (namespace key)
                       key
                       (keyword (if type-ns
                                  (name type-ns)
                                  (throw+ {:type ::missing-type-key-in-entity :entity entity}))
                                (name key)))
                     (if (and (keyword? value)
                              (not (namespace value))
                              type-ns)
                       (convert-keyword type-ns key value)
                       (convert-type key value))])
                  entity))))

(defn clean-fact [fact]
  (let [[operation entity attribute value] fact]
    (if (#{:db/add :db/retract} operation)
      [operation entity attribute (convert-type attribute value)]
      (cons operation (mapv #(if (map? %)
                           (make-entity-map %)
                           (convert-type attribute %))
                        (rest fact))))))

(defn prepare-tx-data [tx-data]
  (mapv (fn [fact]
          [:dispatch-fact
           (if (map? fact)
             (make-entity-map fact)
             (clean-fact fact))])
        tx-data))

(defn transact [ds tx-data]
  (d/transact (or (:connection ds) ds)
              (prepare-tx-data tx-data)))

(defn transact-async [ds tx-data]
  (d/transact-async (or (:connection ds) ds)
                    (prepare-tx-data tx-data)))

(defn load-db-idents [db]
  (into {}
        (d/q '[:find ?id ?val
               :where [?id :db/ident ?val]]
             db)))

(defn install-schema [conn schema-file]
  @(d/transact conn (concat (datomic-schema/read-schema schema-file))))

(defn connect [uri schema-file & {:keys [install-schema?]}]
  (when install-schema?
    (d/create-database uri)
    (install-schema (d/connect uri) schema-file))
  (let [connection (d/connect uri)]
    {:connection connection
     :id->db-ident (load-db-idents (d/db connection))}))

(defn db [conn]
  (assoc (d/db (:connection conn))
         :id->db-ident (:id->db-ident conn)))

(defn current-sequence-value
  "The current value of the counter"
  [db name]
  (first (d/q '[:find [?value ...]
                :in $ ?name
                :where
                [?counter :sequence-number-generator/name ?name]
                [?counter :sequence-number-generator/counter ?value]]
              db
              name)))

(defn next-sequence-value
  "Invoke a transaction function to increment a named sequence-number-generator and return the new value"
  [conn name]
  (current-sequence-value (:db-after @(d/transact (or (:connection conn) conn) [[:increment-sequence name]])) name))

(defn intern-entity [db entity]
  (into {}
        (map (fn [[key value]]
               [(if (= (namespace key) "db")
                  key
                  (keyword (name key)))

                (cond
                  (and (map? value)
                       (:db/id value))
                  (let [db-id (:db/id value)]
                    (if-let [ident ((:id->db-ident db) db-id)]
                      (keyword (name ident))
                      db-id))

                  (vector? value)
                  (set value)

                  (instance? java.util.Date value)
                  (time-coerce/from-date value)

                  :otherwise value)])
             entity)))

(defn maybe-pull [db thing]
  (if (number? thing)
    (d/pull db '[*] thing)
    thing))

(defn load-entity [db id]
  (let [entity (d/pull db '[*] id)]
    (when (:db/id entity)
      (intern-entity db entity))))

(defn select-entities [query db & args]
  (map #(->> %
             (maybe-pull db)
             (intern-entity db))
       (set (apply d/q query db args))))

(defn assert! [conn entity-or-entities]
  (let [single? (map? entity-or-entities)
        entities (map make-entity-map (if single? [entity-or-entities] entity-or-entities))
        ids (map :db/id entities)
        {:keys [db-after tempids]} @(transact conn entities)]
    ((if single? first identity)
     (mapv #(or (d/resolve-tempid db-after tempids %) %) ids))))
