(ns segmentum.logger
  (:require [segmentum.config :as conf]
            [kezban.core :refer [when-no-aot]]
            [clj-time.coerce :as coerce]
            [clj-time.format :as time]
            [amalloy.ring-buffer :refer [ring-buffer]]
            [clojure.tools.logging :as log])
  (:import (ch.qos.logback.classic Logger)
           (org.slf4j LoggerFactory)
           (ch.qos.logback.core AppenderBase)
           (ch.qos.logback.classic.spi LoggingEvent)))


(defonce logs-q (agent (ring-buffer 1024)))
(defonce appender-exists? (atom false))


(defn logs []
  "System logs, from latest to oldest."
  (rseq @logs-q))


(defn- event->log-data [^LoggingEvent event]
  {:instance-id conf/instance-id
   :timestamp   (->> (.getTimeStamp event)
                  coerce/from-long
                  (time/unparse (time/formatters :date-time)))
   :level       (str (.getLevel event))
   :logger-name (.getLoggerName event)
   :message     (.getMessage event)
   :exception   (some-> event .getThrowableProxy .getCause .getMessage)})


(defn- segmentum-appender []
  (proxy [AppenderBase] []
    (append [event]
      ;; When code block throws an exception, it does not appear in REPL.
      (try
        (send logs-q conj (event->log-data event))
        (catch Exception e
          (println "Could not send the log for segmentum appender: " (.getMessage e))))
      nil)))


(when-no-aot
  (when-not @appender-exists?
    (reset! appender-exists? true)
    (let [root-logger (LoggerFactory/getLogger (Logger/ROOT_LOGGER_NAME))
          appender    (segmentum-appender)]
      (.setContext appender (LoggerFactory/getILoggerFactory))
      (.start appender)
      (.addAppender root-logger appender))))