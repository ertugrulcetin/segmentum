(ns segmentum.transformations.analytics
  (:require [segmentum.transformations.google-analytics :refer [google-analytics-handler]]))


(defn- segmentum-event-params [data]
  (select-keys data [:type :category :action :label :value :customer-id]))


(defn segmentum-integration-routes [params integration-key]
  (case integration-key
    :google (google-analytics-handler params)))


(defn segmentum-event-handler [params integration-key]
  (-> params
    segmentum-event-params
    (segmentum-integration-routes integration-key)))


