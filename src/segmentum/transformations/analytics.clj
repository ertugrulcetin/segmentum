(ns segmentum.transformations.analytics
  (:require [segmentum.transformations.google-analytics :refer [google-analytics-handler]]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))


(defn segmentum-event-params [valid-keys data]
  "This function should return segmentum event params"
  (select-keys data valid-keys))


(defn segmentum-integration-routes [integration-key mappings params]
  (case integration-key
    :google (google-analytics-handler params mappings)))


(defn segmentum-event-handler [params integration-key]
  (let [transform-data (-> (io/resource "transforms/analytics/google.edn")
                         slurp
                         edn/read-string)]
    (->> params
      (cske/transform-keys csk/->kebab-case)
      (segmentum-event-params (:valid-keys transform-data))
      (segmentum-integration-routes integration-key (:mappings transform-data)))))
