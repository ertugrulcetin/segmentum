(ns segmentum.transformations.analytics
  (:require [segmentum.transformations.google-analytics :as google]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def mappings-data (->
                     (io/resource "transforms/analytics.edn")
                     slurp
                     edn/read-string))


(defn segmentum-integration-routes [integration-key mappings params]
  (case integration-key
    :google (google/handler params mappings)))


(defn segmentum-event-handler [params integration-key]
  (segmentum-integration-routes integration-key mappings-data params))
