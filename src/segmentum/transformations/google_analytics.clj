(ns segmentum.transformations.google-analytics
  (:require [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [kezban.core :refer :all]))

(def api-url "https://www.google-analytics.com/collect")

(defay mappings (-> (io/resource "transforms/analytics/google.edn")
                  slurp
                  edn/read-string))


(defn transform [event opts]
  (-> event
    (set/rename-keys @mappings)
    (merge {:v   (:google-analytics-version opts)
            :tid (:google-tracking-id opts)})))


(defn ^:transformer handler [event opts]
  (transform event opts))