(ns segmentum.handler
  (:require
   [segmentum.middleware :refer [wrap-base]]
   [patika.core :refer [get-routes]]
   [segmentum.api.core :refer [not-found]]
   [segmentum.env :refer [defaults]]
   [mount.core :refer [defstate]]))


(defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop ((or (:stop defaults) (fn []))))


(defstate app-routes
  :start
  (get-routes {:resource-ns-path "segmentum.api."
               :not-found-route  'segmentum.api.core/not-found}))


(defn app []
  (wrap-base #'app-routes))