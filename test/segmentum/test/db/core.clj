(ns segmentum.test.db.core
  (:require
   [segmentum.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [segmentum.config :refer [env]]
   [mount.core :as mount]))


#_(use-fixtures
    :once
    (fn [f]
      (mount/start
        #'segmentum.config/env
        #'segmentum.db.core/*db*)
      (migrations/migrate ["migrate"] (select-keys env [:database-url]))
      (f)))


#_(deftest test-users
    (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
      (is (= 1 (db/create-user!
                 t-conn
                 {:id         "1"
                  :first_name "Sam"
                  :last_name  "Smith"
                  :email      "sam.smith@example.com"
                  :pass       "pass"})))
      (is (= {:id         "1"
              :first_name "Sam"
              :last_name  "Smith"
              :email      "sam.smith@example.com"
              :pass       "pass"
              :admin      nil
              :last_login nil
              :is_active  nil}
            (db/get-user t-conn {:id "1"})))))
