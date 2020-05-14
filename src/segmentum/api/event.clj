(ns segmentum.api.event
  (:require [segmentum.util.imports :refer [resource]]
            [segmentum.util.macros :as mc]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [throttler.core :refer [throttle-fn]]
            [clojure.walk :as w]
            [aleph.http :as http]
            [kezban.core :refer :all]))


(defonce stream (s/stream 1024))
(defonce put! (throttle-fn (partial s/put! stream) 1000 :second))

(def h {"User-Agent" "Mozilla/5.0 (Windows NT 6.1;) Gecko/20100101 Firefox/13.0.1"})

(mc/async-loop 1 []
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
                   (d/recur))))


(resource event-processing
  :post ["/v1/event"]
  :content-type :json
  :post! #(->> % :request-data w/keywordize-keys put!)
  :handle-created (fn [ctx] {:success? true}))