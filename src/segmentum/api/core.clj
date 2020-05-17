(ns segmentum.api.core
  (:require [segmentum.util.imports :refer [->ModelValidationException resource]]
            [patika.core :as p]
            [compojure.core :as c]
            [compojure.route :as r]
            [clojure.java.io :as io]))


(resource hello
  :get ["/"]
  :content-type :text
  :handle-ok (fn [ctx] "Hello Segmentum!!"))


(resource deneme
  :get ["/test"]
  :content-type :json
  :handle-ok (fn [ctx]
               (throw (->ModelValidationException "heyoo"))))


(c/defroutes not-found (r/not-found "404!"))
