(ns segmentum.api.event
  (:require [segmentum.util.imports :refer [resource]]
            [segmentum.util.macros :as mc]
            [segmentum.config :as conf]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [throttler.core :refer [throttle-fn]]
            [clojure.walk :as w]
            [aleph.http :as http]
            [kezban.core :refer :all])
  (:import (java.util UUID)))

(def xf (map #(assoc % :arrived_at (System/currentTimeMillis)
                       :id (str (UUID/randomUUID)))))

(defonce stream (s/stream 1024 xf))
(defonce put! (throttle-fn (partial s/put! stream) 1000 :second))

(defonce db-stream (s/stream 1024))
(defonce dest-stream (s/stream 1024))

(s/connect stream db-stream)
(s/connect stream dest-stream)


(def h {"User-Agent" "Mozilla/5.0 (Windows NT 6.1;) Gecko/20100101 Firefox/13.0.1"})

(defn process-db-stream []
  (mc/async-loop 1 [events []]
                 (s/try-take! db-stream ::drained 20 ::timeout)

                 (fn [event]
                   (let [events      (if (= ::timeout event) events (conj events event))
                         bulk-events (take 32 events)]
                     ;; write to db
                     (cond
                       (or (= ::timeout event)
                           (and (not= ::timeout event)
                                (= (count bulk-events) 32)))
                       (do
                         ;;write db
                         (println "Writing events: " bulk-events)
                         (d/recur (vec (drop (count bulk-events) events))))

                       :else (d/recur events))))))


(defn process-dest-stream []
  (mc/async-loop conf/cores []
                 (s/take! stream ::drained)

                 ;; if we got a message, run it through `f`
                 (fn [event]
                   (println " - Event: " event)
                   (if (identical? ::drained event)
                     ::drained
                     (identity event)))

                 (fn [event]
                   (println "New Hey: " event)
                   (http/post "https://www.google-analytics.com/collect"
                              {:headers h :form-params {:ev "ses"}}))

                 ;; wait for the result from `f` to be realized, and
                 ;; recur, unless the stream is already drained
                 (fn [result]
                   (println "Result: " (select-keys result [:status :body]))
                   (when-not (identical? ::drained result)
                     (d/recur)))))


(resource event-processing
          :post ["/v1/event"]
          :content-type :json
          :post! #(->> % :request-data w/keywordize-keys put!)
          :handle-created (fn [ctx] {:success? true}))