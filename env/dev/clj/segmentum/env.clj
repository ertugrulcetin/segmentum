(ns segmentum.env
  (:require
   [selmer.parser :as parser]
   [clojure.tools.logging :as log]
   [segmentum.dev-middleware :refer [wrap-dev]]))


(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[segmentum started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[segmentum has shut down successfully]=-"))
   :middleware wrap-dev})
