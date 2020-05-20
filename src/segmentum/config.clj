(ns segmentum.config
  (:require
   [cprop.core :refer [load-config]]
   [cprop.source :as source]
   [mount.core :refer [args defstate]])
  (:import (java.util UUID)))

(defonce instance-id (str (UUID/randomUUID)))
(defonce cores (.. Runtime getRuntime availableProcessors))


(defstate env
  :start
  (load-config
    :merge
    [(args)
     (source/from-system-props)
     (source/from-env)]))


(defn- type->cast-fn [type]
  (case type
    :str  str
    :bool #(Boolean/parseBoolean %)
    :int  #(Integer/parseInt %)
    :long #(Long/parseLong %)
    identity))


(defn get-conf
  ([key]
   (get-conf key nil))
  ([key type]
   (get-conf key type nil))
  ([key type default]
   (or ((type->cast-fn type) (key env))
       default)))

(comment
  (get-conf :java-home)
  )