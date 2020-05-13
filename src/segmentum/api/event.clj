(ns segmentum.api.event
  (:require [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as b]
            [clojure.core.async :as a]
            [throttler.core :refer [throttle-chan throttle-fn]]
            [kezban.core :refer :all]))

(defonce bus  (b/event-bus #(s/stream 1024)))
(defonce pub! (throttle-fn (partial b/publish! bus) 1024 :second))
(defonce sub  (b/subscribe bus :google_analytics))

(dotimes [x 50000]
  (pub! :google_analytics {:data x}))

(when-no-aot
  (d/loop []
          (d/chain
            (s/take! sub ::drained)

            ;; if we got a message, run it through `f`
            (fn [msg]
              (println " - Msg: " msg)
              (if (identical? ::drained msg)
                ::drained
                (identity msg)))

            ;; wait for the result from `f` to be realized, and
            ;; recur, unless the stream is already drained
            (fn [result]
              (when-not (identical? ::drained result)
                (d/recur))))))