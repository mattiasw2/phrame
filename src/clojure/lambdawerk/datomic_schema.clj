(ns lambdawerk.datomic-schema
  (:use [clojure.pprint :only [pprint]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [datomic.api :as d]))

(def id-counter (atom 0))

(defn- next-id []
  (swap! id-counter dec))

(def ^:dynamic *enums*)

(defn- expand-attribute-option [entity-name attribute-name option]
  (let [[key value] option
        key (if (namespace key)
              key
              (keyword "db" (name key)))]
    [key
     (if (keyword? value)
       (if (namespace value)
         value
         (keyword (case key
                    :db/cardinality "db.cardinality"
                    :db/unique "db.unique"
                    :db/index "db.index"
                    entity-name)
                  (name value)))
       value)]))

(defn- expand-type [entity-name attribute-name type]
  (let [enum (and (list? type)
                  (= (first type) 'enum))]
    (when enum
      (swap! *enums* conj {:entity-name entity-name :attribute-name attribute-name :values (rest type)}))
    (if (and (keyword? type) (namespace type))
      type
      (keyword "db.type" (if enum "ref" (name type))))))

(defn- expand-attribute-definition [entity-name clause]
  (let [[attribute-name type doc & options] clause]
    (assert type)
    (assert doc)
    (into {:db/id (datomic.db/id-literal [:db.part/db (next-id)])
           :db.install/_attribute :db.part/db
           :db/cardinality :db.cardinality/one
           :db/index true
           :db/valueType (expand-type entity-name attribute-name type)
           :db/doc doc}
          (map (partial expand-attribute-option entity-name attribute-name)
               (partition 2 (concat [:ident (keyword attribute-name)] options))))))

(defn- define-enum [enum-definition]
  (let [{:keys [entity-name attribute-name values]} enum-definition]
    (map (fn [value]
           [:db/add (datomic.db/id-literal [:db.part/user (next-id)])
            :db/ident (if (and (keyword? value) (namespace value))
                        value
                        (keyword (str (name entity-name) "." (name attribute-name)) (name value)))])
         values)))

(defn- defentity [entity-name & attributes]
  (binding [*enums* (atom #{})]
    (let [attributes (doall (map (partial expand-attribute-definition (name entity-name)) attributes))]
      (doall (concat attributes
                     (apply concat (map define-enum @*enums*)))))))

(defn- defun [name params & body]
  (let [options (when (map? (first body))
                  (first body))
        code (if options (rest body) body)]
    [{:db/id (d/tempid :db.part/user)
      :db/ident (keyword name)
      :db/fn (d/function (into {:lang "clojure"
                                :params params
                                :code `(do ~@code)}
                               options))}]))

(defn clause-handler [clause-name]
  (case clause-name
    defentity defentity
    defun defun
    (throw+ {:type ::invalid-schema-definition-clause :clause clause-name})))

(defn read-schema [filename]
  (with-open [f (java.io.PushbackReader. (io/reader filename))]
    (loop [result []]
      (if-let [clause (edn/read {:eof false} f)]
        (recur (into result (apply (clause-handler (first clause))
                                   (rest clause))))
        result))))
