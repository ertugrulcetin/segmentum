(ns segmentum.api.user
  (:require [segmentum.util.imports :refer [resource]]
            [clojure.walk :as w]
            [crypto.password.bcrypt :as password]
            [segmentum.db.core :as db])
  (:import (java.util UUID Date)))


(defn permit [data rule]
  (some-> data
    (select-keys (keys rule))))


(defn permit? [data rule]
  (some->> rule
    (keep (fn [[k v]] (when v k)))
    (every? #(contains? (permit data rule) %))))


(defn write-user-to-db [data]
  (let [password-salt      (UUID/randomUUID)
        encrypted-password (password/encrypt (str password-salt (:password data)))]
    (db/query :create-user! (merge data
                              {:password_salt password-salt
                               :password encrypted-password}))))


(defn control-user-register [data]
  (let [user-param-rule {:name true
                         :surname true
                         :email true
                         :password true}]
    (if (permit? data user-param-rule)
      (write-user-to-db (permit data user-param-rule))
      (throw (ex-info "Missing Parameters" {:type :400})))))


(defn sign-up [data]
  (control-user-register data))


(defn sign-in [data]
  {})


(resource sign-up
  :post ["/sign_up"]
  :content-type :json
  :post! #(->> % :request-data w/keywordize-keys sign-up)
  :handle-created (fn [ctx] {:success? true}))


(resource login
  :post ["/login"]
  :content-type :json
  :post! #(->> % :request-data w/keywordize-keys sign-in)
  :handle-created (fn [ctx] {:success? true}))
