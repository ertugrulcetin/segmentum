(ns segmentum.transformations.google-analytics
  (:require [clojure.set :as set]
            [clj-http.client :as client]))


;;
;; Google Analytics Tracking id
;;


(def google-analytics-version 1)
(def google-tracking-id "UA-165839048-1")
(def h {"User-Agent" "Mozilla/5.0 (Windows NT 6.1;) Gecko/20100101 Firefox/13.0.1"})


(defn send-google-analytics [params]
  (client/post "https://www.google-analytics.com/collect"
    {:headers h
     :form-params params}))


(defn google-analytics-params [params mappings]
  (-> params
    (set/rename-keys mappings)
    (merge {:v google-analytics-version
            :tid google-tracking-id})))


(defn google-analytics-handler [params mappings]
  (send-google-analytics
    (google-analytics-params params mappings)))
