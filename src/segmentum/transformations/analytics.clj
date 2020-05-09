(ns segmentum.transformations.analytics
  (:require [segmentum.transformations.google-analytics :refer [google-analytics-handler]]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))


(defn segmentum-event-params [data]
  "This function should return segmentum event params"
  (select-keys data [:type :category :action :label :value :client-id
                     :anonymize-ip :data-source :user-id :session-control]))


(defn segmentum-integration-routes [integration-key params]
  (case integration-key
    :google (google-analytics-handler params)))


(defn segmentum-event-handler [params integration-key]
  (->> params
    (cske/transform-keys csk/->kebab-case)
    segmentum-event-params
    (segmentum-integration-routes integration-key)))


