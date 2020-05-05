(ns segmentum.api.core
  (:require [patika.core :refer [resource]]
            [compojure.core :as c]
            [compojure.route :as r]))


(c/defroutes not-found
             (r/not-found "404!"))


(resource hello
          :get ["/"]
          :content-type :text
          :handle-ok (fn [ctx] "Hello Segmentum!!"))