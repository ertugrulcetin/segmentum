(ns segmentum.api.util
  (:require [patika.core :as p]
            [com.rpl.defexception :refer [defexception]]
            [liberator.representation :refer [ring-response]]
            [clojure.tools.logging :as log]))


(defexception ModelValidationException)


(defmacro resource
  [name method endpoint-and-binding _ media-type & opts]
  (let [handle-ex-fn (fn [ctx]
                       (let [throwable (-> ctx :exception Throwable->map)]
                         (log/error throwable)
                         (ring-response
                           (if (instance? ModelValidationException (:exception ctx))
                             {:status 400 :body (:cause throwable)}
                             {:status 500 :body "Something went wrong"}))))]
    `(p/resource ~name
                 ~method
                 ~endpoint-and-binding
                 ~'_
                 ~media-type
                 :handle-exception ~handle-ex-fn
                 ~@opts)))