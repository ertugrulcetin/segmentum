(ns segmentum.transformations.google-analytics
  (:require [clj-http.client :as client]
            [segmentum.transformations.helper :as helper]))


(defn send-google-analytics [params]
  (client/post "https://www.google-analytics.com/collect"
    {:form-params params}))


(defn ^:transformer handler [params mappings-data]
  (send-google-analytics
    (helper/data-transform mappings-data params :ga)))
