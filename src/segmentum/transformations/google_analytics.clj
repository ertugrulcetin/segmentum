(ns segmentum.transformations.google-analytics
  (:require [segmentum.transformations.helper :as helper]
            [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [kezban.core :refer :all]))

(def api-url "https://www.google-analytics.com/collect")


(defay mappings (-> (io/resource "transforms/analytics.edn")
                  slurp
                  edn/read-string))


;;TODO ???
#_(defn transform [event opts]
    (-> event
      (set/rename-keys @mappings)
      (merge {:v   (:google-analytics-version opts)
              :tid (:google-tracking-id opts)})))


(defn ^{:transformer :google-analytics} transform
  ([event]
   (transform event {}))
  ([event opts]
   ;;TODO opts ekle!
   {:url    api-url
    :params (helper/data-transform @mappings event :ga)}))