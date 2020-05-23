(ns segmentum.middleware.session
  (:require [segmentum.api.common :refer [*source* *destinations*]]
            [segmentum.db.core :as db]
            [clojure.string :as str]))


(defn do-with-destinations [uri write-key thunk]
  ;;TODO do not forget the change path, if we change the event path
  (if (and (= "/v1/event" uri)
        (not (str/blank? write-key)))
    (let [source       (delay (db/query :get-source-by-write-key {:write_key write-key}))
          destinations (delay (db/query :get-destinations-by-source-id {:source_id (:id source)}))]
      (binding [*source*       source
                *destinations* destinations]
        (thunk)))
    (thunk)))


(defmacro with-destination-for-request [request & body]
  `(let [request#   ~request
         uri#       (:uri request#)
         write-key# (-> request# :headers (get "x-write-key"))]
     (do-with-destinations uri# write-key# (fn [] ~@body))))


(defn bind-destinations [handler]
  (fn
    ([request]
     (with-destination-for-request request
       (handler request)))
    ([request respond raise]
     (with-destination-for-request request
       (handler request respond raise)))))