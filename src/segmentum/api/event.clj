(ns segmentum.api.event
  (:require [segmentum.util.imports :refer [resource]]
            [segmentum.util.macros :as mc]
            [segmentum.config :as conf]
            [segmentum.db.core :as db]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [throttler.core :refer [throttle-fn]]
            [clojure.walk :as w]
            [aleph.http :as http]
            [kezban.core :refer :all]
            [nano-id.core :refer [nano-id]]
            [clojure.tools.logging :as log])
  (:import (java.util UUID Date)))


(def xf (map #(assoc % :arrived_at (Date.) :id (UUID/randomUUID))))
(defonce stream (s/stream 1024 xf))
(defonce put! (throttle-fn (partial s/put! stream) 1000 :second))

(def db-xf (map #(vector (:id %) (:write_key %) (:payload %) (:arrived_at %))))
(defonce db-stream (s/stream 1024 db-xf))
(defonce dest-stream (s/stream 1024))

(def fail-xf (map #(assoc % :retry 0)))
(defonce failed-write-stream (s/stream 512 fail-xf))

(s/connect stream db-stream)
;;TODO backpressure'a neden oluyor
;(s/connect stream dest-stream)


(defn- get-db-fn-by-type [type]
  (case type
    :raw (partial db/query :create-event!)))


(defn- try-write-to-db
  ([event events]
   (try-write-to-db event events true))
  ([event events new-event?]
   (try
     ((get-db-fn-by-type (:type event)) (dissoc event :type))
     (log/info "Fail write successfully written to DB: " event)
     (-> events rest vec d/recur)
     (catch Exception e
       (log/error e "Retry write failed.")
       (if new-event?
         (d/recur (conj events (update event :retry inc)))
         (d/recur (conj (vec (rest events)) (update event :retry inc))))))))


(defn- process-failed-db-writes []
  (mc/async-loop 1 [events []]
    (d/timeout!
      (s/take! failed-write-stream ::drained)
      1000
      ::timeout)
    (fn [data]
      (if-not (#{::drained ::timeout} data)
        (try-write-to-db data events)
        (if-let [f-event (first events)]
          (if (>= (:retry f-event) 3)
            (do
              (log/warn "Discarding event due to max number of retry count." f-event)
              (-> events rest vec d/recur))
            (try-write-to-db f-event events false))
          (d/recur []))))))


(defn- process-db-stream []
  (s/consume-async (fn [events]
                     (try
                       (log/info "Writing events to DB" events)
                       (db/query :create-events! {:events events})
                       (catch Exception e
                         (log/error e "Failed to write events to DB!")
                         (s/put-all! failed-write-stream (map
                                                           (fn [[id write-key payload arrived-at]]
                                                             {:id         id
                                                              :write_key  write-key
                                                              :payload    payload
                                                              :arrived_at arrived-at
                                                              :type       :raw})
                                                           events)))))
                   ;;TODO make configurable
    (s/batch 32 100 db-stream)))


(comment
  (dotimes [_ 5000]
    (put! {:id        (UUID/randomUUID)
           :write_key (nano-id 32)
           :payload   {:data (rand-int 1000)}}))
  (process-db-stream)
  (process-failed-db-writes))


(defn- process-dest-stream []
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
      event
      #_(http/post "https://www.google-analytics.com/collect" {:headers h :form-params {:ev "ses"}}))

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