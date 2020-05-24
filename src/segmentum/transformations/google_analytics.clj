(ns segmentum.transformations.google-analytics
  (:require [segmentum.transformations.helper :as helper]
            [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [kezban.core :refer :all]))

(def api-url "https://www.google-analytics.com/collect")


;;TODO ???
#_(defn transform [event opts]
    (-> event
      (set/rename-keys @mappings)
      (merge {:v   (:google-analytics-version opts)
              :tid (:google-tracking-id opts)})))


(defn ^{:transformer :google-analytics} transform
  ([params]
   (transform params {}))
  ([params config]
   ;;TODO config ekle!
   {:url    api-url
    :payload (helper/data-transform params :ga)}))