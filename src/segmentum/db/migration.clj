(ns segmentum.db.migration
  (:require [migratus.core :as migratus]))


(defn start-migration
  [db-uri]
  (let [config {:store         :database
                :migration-dir "migrations/"
                :db            db-uri}]
    (migratus/migrate config)
    (migratus/up config 20200505083606)))