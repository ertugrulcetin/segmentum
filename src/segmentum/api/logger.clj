(ns segmentum.api.logger
  (:require [segmentum.config :as conf]
            [segmentum.util.imports :refer [resource]]
            [kezban.core :refer [when-no-aot]]
            [clj-time.coerce :as coerce]
            [clj-time.format :as time]
            [amalloy.ring-buffer :refer [ring-buffer]])
  (:import (org.slf4j LoggerFactory)
           (ch.qos.logback.classic Logger)
           (ch.qos.logback.core AppenderBase)
           (ch.qos.logback.classic.spi LoggingEvent)))


;;TODO let's make 1024 configurable
(defonce logs-q (agent (ring-buffer 1024)))
(defonce appender-exists? (atom false))


(defn logs []
  "System logs, from newest to oldest."
  (rseq @logs-q))


(defn- event->log-data [^LoggingEvent event]
  {:instance-id conf/instance-id
   :timestamp   (->> (.getTimeStamp event)
                  coerce/from-long
                  (time/unparse (time/formatter :date-time)))
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


;;TODO auth!
(resource sys-logs
  :get ["/logs"]
  :content-type :json
  :handle-ok (fn [_] (logs)))