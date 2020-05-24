(ns segmentum.api.event
  (:require [segmentum.util.imports :refer [resource]]
            [segmentum.util.macros :as mc]
            [segmentum.config :as conf :refer [env]]
            [segmentum.db.core :as db]
            [segmentum.api.common :refer [*source* *destinations*]]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [throttler.core :refer [throttle-fn]]
            [clojure.walk :as w]
            [aleph.http :as http]
            [kezban.core :refer :all]
            [nano-id.core :refer [nano-id]]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate]]
            [clojure.java.classpath :as classpath]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:import (java.util UUID Date)))


(defay transformers
  (->> (for [ns-sym (ns-find/find-namespaces (classpath/system-classpath))
             :let [starts-with? (partial str/starts-with? (name ns-sym))]
             :when (and (starts-with? "segmentum.transformations.")
                     (find-ns ns-sym))]
         (some #(when (:transformer (meta %)) [(-> % meta :transformer) %])
           (vals (ns-publics ns-sym))))
    (filter identity)
    (into {})))


(when-no-aot
  (log/info "Found transformers: " (keys @transformers)))


;;TODO randomUUID, what if other instances produce the same ID?
(def xf (map #(assoc % :arrived_at (Date.) :id (UUID/randomUUID))))
(defonce stream (s/stream* {:permanent? true :buffer-size 1024 :xform xf}))
(defonce put! (throttle-fn (partial s/put! stream) 1000 :second))


;;TODO write_key comes with http header...
(def db-xf (map #(vector (:id %) (:write_key %) (:payload %) (:arrived_at %))))
(defonce db-stream (s/stream 1024 db-xf))
(defonce dest-stream (s/stream 2048))


(def fail-xf (map #(update % :retry (fnil inc -1))))
(defonce failed-write-stream (s/stream 512 fail-xf))


(defn- get-db-fn-by-type [type]
  (let [k (case type
            :raw :create-event!
            :fail :create-fail-event!
            :success :create-success-event!)]
    (partial db/query k)))


(defn- try-write-to-db
  [event]
  (try
    (if (>= (:retry event) 3)
      (log/warn "Discarding event due to max number of retry attempt." event)
      (do
        ((get-db-fn-by-type (:type event)) (dissoc event :type))
        (log/info "Fail write successfully written to DB: " event)))
    (catch Exception e
      (log/error e "Retry write failed for event: " event)
      (s/put! failed-write-stream event))))


(defn- process-failed-db-writes []
  (mc/async-loop 1 []
    (s/take! failed-write-stream ::drained)

    (fn [data]
      (when-not (#{::drained} data)
        (try-write-to-db data)
        (Thread/sleep 1000)
        (d/recur)))))


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
   :destination_id  (UUID/randomUUID)
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
          (let [transformer (:transformer event)
                {:keys [url params]} (transformer event (:config event))
                event (dissoc event :transformer :config)]
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


(defn- event->db-write [event]
    (s/put! db-stream (assoc event :write_key (:write_key @*source*)
                                   :payload (dissoc event :id :arrived_at))))


(defn- event->destination-events [event]
  (->> @*destinations*
    (map (fn [d]
           (assoc event
             :transformer ((keyword (:type d)) @transformers)
             :config (:config d))))
    (s/put-all! dest-stream)))


(defn- init-stream-processing []
  (s/connect-via stream event->db-write db-stream)
  (s/connect-via stream event->destination-events dest-stream)
  (process-db-stream)
  (process-failed-db-writes)
  (process-dest-stream))


;;TODO check dest-stream backpressure!
(defstate ^{:on-reload :noop} streams
  :start
  (init-stream-processing))


(comment
  (dotimes [_ 1]
    (put! {:write_key (nano-id 32)
           :payload   {:data (rand-int 1000)}})))


(resource event-processing
  :post ["/v1/event"]
  :content-type :json
  :post! (fn [ctx]
           (cond
             (not @*source*) (throw (ex-info "No source found for given key" {:type :400}))
             (not @*destinations*) (throw (ex-info "No destination found for given source" {:type :400}))
             ;;TODO add validation? maybe optional, configured by admins
             :else (->> ctx :request-data w/keywordize-keys put!)))
  :handle-created (fn [_] {:success? true}))


;;TODO add admin auth!
;;TODO add executor that logs/info every 1 min?
(resource stream-monitoring
  :get ["/streams"]
  :content-type :json
  :handle-ok (fn [_]
                       ;; update here according to streams
               (->> {:events           (.description stream)
                     :destinations     (.description dest-stream)
                     :db               (.description db-stream)
                     :failed-db-writes (.description failed-write-stream)}
                 pp/pprint
                 with-out-str
                 (str "If we have more than 16.000 pending takes or puts which means we are in trouble (System throws and exception after 16.3k pending operations and halts). You can optimize stream operations by tuning configuration.\n\n"))))