(ns segmentum.util.macros
  (:require [manifold.deferred :as d]
            [kezban.core :refer :all]))


(defmacro async-loop [parallelism bindings & body]
  `(when-no-aot
     (dotimes [_# ~parallelism]
       (d/loop ~bindings
         (d/chain ~@body)))))