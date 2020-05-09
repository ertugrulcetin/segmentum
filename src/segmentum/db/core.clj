(ns segmentum.db.core
  (:require
   [cheshire.core :refer [generate-string parse-string]]
   [next.jdbc.date-time]
   [next.jdbc.prepare]
   [next.jdbc.result-set]
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [conman.core :as conman]
   [honeysql.core :as hsql]
   [segmentum.config :refer [env]]
   [mount.core :refer [defstate]]
   [clojure.java.jdbc :as jdbc]
   [hugsql.core :as hugsql]
   [hugsql.adapter.next-jdbc :as next-adapter]
   [clojure.java.io :as io])
  (:import (org.postgresql.util PGobject)))


;;TODO we might want to add delay fn to avoid execute conn during compiling
;;TODO add pool spec instead of :jdbc-url
(defstate ^:dynamic *db*
  :start (if-let [jdbc-url (env :database-url)]
           (do
             (log/info "Establishing database connection...")
             (let [conn (conman/connect! {:jdbc-url jdbc-url})]
               (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))
               conn))
           (do
             (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
             *db*))
  :stop (conman/disconnect! *db*))


(defn- get-sql-files []
  (->> (io/resource "sql")
    io/as-file
    file-seq
    (filter (memfn isFile))))


(defstate conn-map
  :start (apply conman/bind-connection-map (cons *db* (get-sql-files))))


(defn pgobj->clj [^org.postgresql.util.PGobject pgobj]
  (let [type  (.getType pgobj)
        value (.getValue pgobj)]
    (case type
      "json" (parse-string value true)
      "jsonb" (parse-string value true)
      "citext" (str value)
      value)))


(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v))
  java.sql.Array
  (read-column-by-label [^java.sql.Array v _]
    (vec (.getArray v)))
  (read-column-by-index [^java.sql.Array v _2 _3]
    (vec (.getArray v)))
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject pgobj _]
    (pgobj->clj pgobj))
  (read-column-by-index [^org.postgresql.util.PGobject pgobj _2 _3]
    (pgobj->clj pgobj)))


(defn clj->jsonb-pgobj [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))


(extend-protocol next.jdbc.prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [^clojure.lang.IPersistentMap v ^java.sql.PreparedStatement stmt ^long idx]
    (.setObject stmt idx (clj->jsonb-pgobj v)))

  clojure.lang.IPersistentVector
  (set-parameter [^clojure.lang.IPersistentVector v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_) (str/join (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (clj->jsonb-pgobj v))))))


(defn query
  ([q-name]
   (query q-name {}))
  ([q-name params]
   (conman/query conn-map q-name params)))


(defmacro with-trans
  "Executes a set of SQL queries, if one fails all system rolls back."
  [& args]
  `(conman/with-transaction [*db*]
     ~@args))


(defn exec-query
  "Takes a query as a string (Raw SQL) to evaluate.
   Usage: `(exec-query \"SELECT * FROM Customers;\")`"
  [sql]
  (jdbc/with-db-connection [conn {:datasource *db*}]
    (jdbc/query conn sql)))


(defn exec-query-in-hsql
  "Takes clauses as a set of keywords, HoneySQL generates the SQL form.
   Usage: `(exec-query-in-hsql :select :* :from :customers)`
   Detailed docs: https://github.com/jkk/honeysql"
  [& clauses]
  (jdbc/with-db-connection [conn {:datasource *db*}]
    (jdbc/query conn (hsql/format (apply hsql/build clauses)))))