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

(def failed-write-xf (map #(assoc % :retry 0)))
(defonce failed-write-stream (s/stream 512 failed-write-xf))


(s/connect stream db-stream)
;;TODO backpressure'a neden oluyor
;(s/connect stream dest-stream)


(def h {"User-Agent" "Mozilla/5.0 (Windows NT 6.1;) Gecko/20100101 Firefox/13.0.1"})


(defn- write-to-db? [event bulk-events]
  (or (and (= ::timeout event)
        (pos? (count bulk-events)))
    (and (not= ::timeout event)
      (= (count bulk-events) 32))))


(defn- get-db-fn-by-type [type]
  (case type
    :event (partial db/query :create-event!)))


(defn process-failed-db-writes []
  (mc/async-loop 1 [events []]
    (s/try-take! failed-write-stream ::drained 10000 ::timeout)

    (fn [event]
      (cond
        (= event ::timeout)
        (if-let [event (first events)]
          (try
            (if (>= (:retry event) 3)
              (do
                (log/info "Event: " event " is going to be discarded due to exceeding number of tries.")
                (d/recur (vec (rest events))))
              (do
                ((get-db-fn-by-type (:type event)) (dissoc event :type))
                (d/recur (vec (rest events)))))
            (catch Exception e
              (log/error e "Could not write event to DB." event)
              (d/recur (vec (rest (conj events (update event :retry inc)))))))
          (d/recur []))

        (not= event ::drained)
        (try
          ((get-db-fn-by-type (:type event)) (dissoc event :type))
          (log/info "Failed db write successfully written to DB.")
          (d/recur events)
          (catch Exception e
            (log/error e "Could not write event to DB.")
            (d/recur (conj events (update event :retry inc)))))

        :else (d/recur events)))))


(defn process-db-stream []
  (mc/async-loop 1 [events []]
    (s/try-take! db-stream ::drained 20 ::timeout)

    (fn [event]
      (let [events      (if (= ::timeout event) events (conj events event))
            bulk-events (take 32 events)]
        (if (write-to-db? event bulk-events)
          (try
            (log/debug "Writing events: " bulk-events)
            (db/query :create-events! {:events bulk-events})
            (->> events (drop (count bulk-events)) vec d/recur)
            (catch Exception e
              (log/error e "Could not write events to DB." bulk-events)
              (doseq [[id write-key payload arrived-at] bulk-events]
                (s/put! failed-write-stream {:id               id
                                             :write_key  write-key
                                             :arrived_at arrived-at
                                             :payload    payload
                                             :type       :event}))
              (-> (drop (count bulk-events) events) vec d/recur)))
          (d/recur events))))))


(comment
  (db/query :create-event! {:id (UUID/randomUUID)
                            :arrived_at (Date.)
                            :payload {:data 23}
                            :write_key "12312asd2a"})
  (dotimes [_ 10]
    (put! {:id        (UUID/randomUUID)
           :write_key (nano-id 32)
           :payload   {:data (rand-int 1000)}}))
  (process-db-stream)
  (process-failed-db-writes))


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