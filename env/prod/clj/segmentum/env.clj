(ns segmentum.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[segmentum started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[segmentum has shut down successfully]=-"))
   :middleware identity})
