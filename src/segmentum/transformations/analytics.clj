(ns segmentum.transformations.analytics
  (:require [segmentum.transformations.google-analytics :refer [google-analytics-handler]]))


(defn segmentum-event-params [data]
  ^{:doc [{:param "anonymize-ip" :type :bool :values [true false]}
          {:param "data-source" :type :text :values ["web" "app" "call center" "crm"]}
          {:param "client-id" :type :text}
          {:param "session-control" :type :text :values ["start" "end"]}]}
  (select-keys data [:type :category :action :label :value :client-id
                     :anonymize-ip :data-source :user-id :session-control]))


(defn segmentum-integration-routes [params integration-key]
  (case integration-key
    :google (google-analytics-handler params)))


(defn segmentum-event-handler [params integration-key]
  (-> params
    segmentum-event-params
    (segmentum-integration-routes integration-key)))


