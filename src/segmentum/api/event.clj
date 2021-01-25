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
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:import (java.util UUID Date)))

;;TODO adjust logs...!

(defay transformers
  (->> (for [ns-sym (ns-find/find-namespaces (classpath/system-classpath))
             :let [starts-with? (partial str/starts-with? (name ns-sym))]
             :when (and (starts-with? "segmentum.transformations.")
                     (find-ns ns-sym))]
         (some #(when-let [t (:transformer (meta %))] [t %])
           (vals (ns-publics ns-sym))))
    (filter identity)
    (into {})))


(when-no-aot
  (log/info "Found transformers: " (keys @transformers)))


;;TODO randomUUID, what if other instances produce the same ID?
(def xf (map #(assoc % :arrived_at (Date.) :id (UUID/randomUUID))))
(defonce stream (s/stream* {:permanent? true :buffer-size 1024 :xform xf}))
(defonce put! (throttle-fn (partial s/put! stream) 1000 :second))


(def db-xf (map #(vector (:id %) (:write_key %) (:params %) (:arrived_at %))))
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
                                                           (fn [[id write-key params arrived-at]]
                                                             {:id         id
                                                              :write_key  write-key
                                                              :params     params
                                                              :arrived_at arrived-at
                                                              :type       :raw})
                                                           events)))))
                   ;;TODO make configurable
    (s/batch 32 100 db-stream)))


(defn- result->event-model [result]
  {:write_key       (-> result :event :write_key)
   :destination_id  (-> result :event :destination_id)
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


(defn result->map [result]
  (let [body (-> (:body result)
               (or "")
               bs/to-string
                 ; PostgreSQL does not allow '\u0000' unicode
               (str/replace "\u0000" "")
               (str/replace "\\u0000" ""))]
    (try
      (assoc result :body (json/read-str body))
      (catch Exception e
        (log/warn e "Response body could not parsed into JSON form.")
        (assoc result :body body)))))


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
                {:keys [url payload]} (transformer (:params event) (:config event))
                event       (dissoc event :transformer :config)]
            (d/timeout!
              (d/chain'
                (http/post url {:form-params payload})
                #(assoc % :event event :payload payload))
              1000
              {::timeout true
               :event    event
               :payload  payload}))))

      (fn [result]
        (if (and (not (::timeout result)) (not= ::drained result))
          (result->map result)
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


(defn- event->destination-events [event]
  (->> @*destinations*
    (map (fn [d]
           (assoc event
             :transformer ((keyword (:type d)) @transformers)
             :config (:config d)
             :destination_id (:id d))))
    (s/put-all! dest-stream)))


(defn- init-stream-processing []
  (s/connect stream db-stream)
  (s/connect-via stream event->destination-events dest-stream)
  (process-db-stream)
  (process-failed-db-writes)
  (process-dest-stream))


;;TODO check dest-stream backpressure!
(defstate ^{:on-reload :noop} streams
  :start
  (init-stream-processing))


(resource event-processing
  :post ["/v1/event"]
  :content-type :json
  :post! (fn [ctx]
           (cond
             (not @*source*) (throw (ex-info "No source found for given key" {:type :400}))
             (not @*destinations*) (throw (ex-info "No destination found for given source" {:type :400}))
                     ;;TODO add validation? maybe optional, configured by admins
             :else (->> ctx
                     :request-data
                     w/keywordize-keys
                     (hash-map :write_key (:write_key @*source*) :params)
                     put!)))
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
                 (str "If we have more than 16.000 pending takes or puts which means we are in trouble "
                   "(System throws and exception after 16.3k pending operations and halts). "
                   "You can optimize stream operations by tuning configuration.\n\n"))))
