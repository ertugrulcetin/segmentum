(ns segmentum.config
  (:require
   [cprop.core :refer [load-config]]
   [cprop.source :as source]
   [mount.core :refer [args defstate]])
  (:import (java.util UUID)))


(defonce instance-id (str (UUID/randomUUID)))


(defstate env
  :start
  (load-config
    :merge
    [(args)
     (source/from-system-props)
     (source/from-env)]))
