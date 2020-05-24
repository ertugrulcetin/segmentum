(ns segmentum.api.common
  (:require [patika.core :as p]
            [liberator.representation :refer [ring-response]]
            [clojure.tools.logging :as log]))

(def ^:dynamic *current-user* (atom nil))
(def ^:dynamic *source* (atom nil))
(def ^:dynamic *destinations* (atom nil))


(defmacro resource
  [name method endpoint-and-binding _ media-type & opts]
  (let [handle-ex-fn (fn [ctx]
                       (let [throwable (-> ctx :exception Throwable->map)]
                         (log/error throwable)
                         (ring-response
                           (if (-> throwable :data :type (= :400))
                             {:status 400 :body (:cause throwable)}
                             {:status 500 :body "Something went wrong"}))))]
    `(p/resource ~name
       ~method
       ~endpoint-and-binding
       ~'_
       ~media-type
       :handle-exception ~handle-ex-fn
       ~@opts)))