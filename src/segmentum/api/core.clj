(ns segmentum.api.core
  (:require [segmentum.util.imports :refer [->ModelValidationException resource]]
            [patika.core :as p]
            [compojure.core :as c]
            [compojure.route :as r]
            [clojure.java.io :as io]
            [segmentum.transformations.analytics :refer [segmentum-event-handler]]))


(resource hello
  :get ["/"]
  :content-type :text
  :handle-ok (fn [ctx] "Hello Segmentum!!"))


(resource deneme
  :get ["/test"]
  :content-type :json
  :handle-ok (fn [ctx]
               (throw (->ModelValidationException "heyoo"))))


(resource api-event
          :post ["/v1/event"]
          :content-type :json
          :post! (fn [ctx]
                   (let [data (clojure.walk/keywordize-keys (:request-data ctx))]
                     (segmentum-event-handler data :google)))
          :handle-created (fn [ctx] {:success? true}))


(resource event
  :post ["/event/google"]
  :content-type :json
  :post! (fn [ctx]
           (let [data (clojure.walk/keywordize-keys (:request-data ctx))]
             (segmentum-event-handler data :google)))
  :handle-created (fn [ctx] {:success? true}))


(c/defroutes not-found (r/not-found "404!"))
