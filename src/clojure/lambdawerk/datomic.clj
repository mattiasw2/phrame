(ns lambdawerk.datomic
  (:require [clj-time.coerce :as time-coerce]
            [datomic.api :as d]
            [lambdawerk.datomic-schema :as datomic-schema]))

(defn convert-type [attribute-name obj]
  (or (if (instance? org.joda.time.DateTime obj)
        (time-coerce/to-date obj)
        obj)
      (throw (Exception. (str "unexpected nil value for attribute " attribute-name)))))

(defn make-entity-map [entity]
  (let [type-ns (:<type> entity)]
    (into {:db/id (or (:db/id entity)
                      (d/tempid :db.part/user))}
          (map (fn [[key value]]
                 [(if (namespace key)
                    key
                    (keyword (if type-ns
                               (name type-ns)
                               (throw (Exception. (str "missing :<type> key in entity " entity))))
                             (name key)))
                  (convert-type key value)])
               (dissoc entity :<type>)))))

(defn clean-fact [fact]
  (let [[operation entity attribute value] fact]
    (if (#{:db/add :db/retract} operation)
      [operation entity attribute (convert-type attribute value)]
      (cons (operation fact) (mapv (partial convert-type attribute) (rest fact))))))

(defn transact [ds tx-data]
  (d/transact (or (:connection ds) ds)
              (map #(if (map? %)
                      (make-entity-map %)
                      (clean-fact %))
                   tx-data)))

(defn load-db-idents [db]
  (into {}
        (d/q '[:find ?id ?val
               :where [?id :db/ident ?val]]
             db)))

(defn install-schema [conn schema-file]
  @(transact conn (concat (datomic-schema/read-schema schema-file)
                          [{:db/id (d/tempid :db.part/user)
                            :db/ident :increment-sequence
                            :db/fn (d/function
                                    {:lang "clojure"
                                     :params '[ds name]
                                     :code '(let [e (d/entity ds [:sequence-number-generator/name name])
                                                  new-counter (inc (:sequence-number-generator/counter e 0))]
                                              [{:db/id (:db/id e (d/tempid :db.part/user))
                                                :sequence-number-generator/name name
                                                :sequence-number-generator/counter new-counter}])})}])))

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

(defn pull-intern [db id]
  (into {}
        (map (fn [[key value]]
               [(keyword (name key)) (cond
                                       (and (map? value)
                                            (:db/id value))
                                       (if-let [ident ((:id->db-ident db) (:db/id value))]
                                         (keyword (name ident))
                                         (:db/id value))

                                       (vector? value)
                                       (set value)

                                       (instance? java.util.Date value)
                                       (time-coerce/from-date value)

                                       :otherwise value)])
             (d/pull db '[*] id))))

(defn select-entities [query db & args]
   (map (partial pull-intern db)
        (set (apply d/q query db args))))

(defn assert! [conn entity-or-entities]
  (let [single? (map? entity-or-entities)
        entities (map make-entity-map (if single? [entity-or-entities] entity-or-entities))
        ids (map :db/id entities)
        {:keys [db-after tempids]} @(transact conn entities)]
    ((if single? first identity)
     (mapv #(or (d/resolve-tempid db-after tempids %) %) ids))))
