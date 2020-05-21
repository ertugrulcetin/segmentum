(ns segmentum.api.event
  (:require [segmentum.util.imports :refer [resource]]
            [segmentum.util.macros :as mc]
            [segmentum.config :as conf :refer [env]]
            [segmentum.db.core :as db]
            [segmentum.transformations.google-analytics :as trans.ga]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [throttler.core :refer [throttle-fn]]
            [clojure.walk :as w]
            [aleph.http :as http]
            [kezban.core :refer :all]
            [nano-id.core :refer [nano-id]]
            [clojure.tools.logging :as log])
  (:import (java.util UUID Date)))


(def xf (map #(assoc % :arrived_at (Date.) :id (UUID/randomUUID))))
(defonce stream (s/stream* {:permanent? true :buffer-size 1024 :xform xf}))
(defonce put! (throttle-fn (partial s/put! stream) 1000 :second))


(def db-xf (map #(vector (:id %) (:write_key %) (:payload %) (:arrived_at %))))
(defonce db-stream (s/stream 1024 db-xf))
(defonce dest-stream (s/stream 1024))


(def fail-xf (map #(assoc % :retry 0)))
(defonce failed-write-stream (s/stream 512 fail-xf))


(s/connect stream db-stream)
;;TODO check backpressure!
(s/connect stream dest-stream)


(defn- get-db-fn-by-type [type]
  (let [k (case type
            :raw :create-event!
            :fail :create-fail-event!
            :success :create-success-event!)]
    (partial db/query k)))


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
              (log/warn "Discarding event due to max number of retry attempt." f-event)
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


(defn- result->event-model [result]
  {:write_key       (-> result :event :write_key)
   ;;TODO destination_id
   :destination_id  1
   :arrived_at      (-> result :event :arrived_at)
   :event_id        (-> result :event :id)
   :request_payload (:payload result)
   :response        (dissoc result :event :payload)})


(defn- process-fail-result
  ([result]
   (process-fail-result result false))
  ([result timeout?]
   (let [event (assoc (result->event-model result) :timeout timeout?)]
     (try
       (db/query :create-fail-event! event)
       (catch Exception e
         (log/error e "Failed to write fail event to DB - " event)
         (s/put! failed-write-stream (assoc event :type :fail)))))))


(defn- process-success-result [result]
  (let [event (result->event-model result)]
    (try
      (db/query :create-success-event! event)
      (catch Exception e
        (log/error e "Failed to write success event to DB - " event)
        (s/put! failed-write-stream (assoc event :type :success))))))


;;TODO ilk eventi islemiyor status falan nil donuyor, sonrakiler calisiyor...
(defn- process-dest-stream []
  (let [parallelism (if (:prod env) conf/cores 1)]
    (mc/async-loop parallelism []
      (s/take! dest-stream ::drained)

      (fn [event]
        (log/info " - Event: " event)
        (if (identical? ::drained event)
          ::drained
                       ;;TODO find suitable handler according to event data
          (let [{:keys [url params]} (trans.ga/handler event)]
            (d/timeout!
              (d/chain'
                (http/post url {:form-params params})
                #(assoc % :event event :payload params))
              1000
              {::timeout true
               :event    event
               :payload  params}))))

      (fn [result]
        (if (and (not (::timeout result)) (not= ::drained result))
          (update result :body bs/to-string)
          result))

      (fn [result]
        (log/info "Result: " result)
        (when-not (identical? ::drained result)
          (cond
            (::timeout result)
            (process-fail-result result true)

            (<= 200 (:status result) 299)
            (process-success-result result)

            :else (process-fail-result result))
          (d/recur))))))


(comment
  (let [e      {:id         #uuid "598f7f22-f8b7-4b14-9923-e7eb4c64dc5b"
                :write_key  "s4SjR-KgN1KBLMJkMLVUluL0y-rS9oZA"
                :payload    {:data 291}
                :arrived_at #inst "2020-05-21T17:41:11.142-00:00"}
        result @(http/post "https://www.google-analytics.com/collect"
                  {:form-params e})])

  (let [r @(http/get "https://jsonplaceholder.typicode.com/todos/1")]
    (byte-streams/to-string (:body r)))

  (dotimes [_ 1]
    (put! {:id        (UUID/randomUUID)
           :write_key (nano-id 32)
           :payload   {:data (rand-int 1000)}}))
  (process-db-stream)
  (process-failed-db-writes)
  (process-dest-stream))


(resource event-processing
  :post ["/v1/event"]
  :content-type :json
  :post! #(->> % :request-data w/keywordize-keys put!)
  :handle-created (fn [ctx] {:success? true}))