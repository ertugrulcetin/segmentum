(ns segmentum.troubleshooting
  (:require [clojure.walk :as walk]
            [segmentum.db.core :as db])
  (:import (java.lang.management ManagementFactory)))


(def props ["file.encoding" "java.class.version" "java.runtime.name"
            "java.runtime.version" "java.vendor" "java.vendor.url"
            "java.version" "java.vm.name" "java.vm.version"
            "jdk.attach.allowAttachSelf" "jdk.debug" "os.arch"
            "os.name" "os.version" "segmentum.version"
            "user.language" "user.timezone"])


(defn system-properties []
  (->> props
    (select-keys (System/getProperties))
    walk/keywordize-keys
    (into (sorted-map))))


(defn db-info []
  (with-open [conn (.getConnection db/*db*)]
    (let [md (.getMetaData conn)]
      {:database    {:name    (.getDatabaseProductName md)
                     :version (.getDatabaseProductVersion md)}
       :jdbc-driver {:name    (.getDriverName md)
                     :version (.getDriverVersion md)}})))


(defn jvm-arguments []
  (filterv #(re-find #"-XX:*|-Xms*|-Xmx|-Xlog*|-server|-client" %)
    (.getInputArguments (ManagementFactory/getRuntimeMXBean))))


(defn system-info []
  (into (sorted-map) {:system-properties (system-properties)
                      :db                (db-info)
                      :jvm-arguments     (jvm-arguments)}))