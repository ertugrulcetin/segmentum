(ns segmentum.core
  (:require
   [segmentum.handler :as handler]
   [segmentum.nrepl :as nrepl]
   [segmentum.db.migration :as mig]
   [segmentum.config :refer [env]]
   [luminus.http-server :as http]
   [luminus-migrations.core :as migrations]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.tools.logging :as log]
   [mount.core :as mount])
  (:gen-class)
  (:import (java.util TimeZone)))


(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log/error {:what      :uncaught-exception
                  :exception ex
                  :where     (str "Uncaught exception on" (.getName thread))}))))


(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])


(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
    (-> env
      (assoc :handler (handler/app))
      (update :port #(or (-> env :options :port) %))))
  :stop
  (http/stop http-server))


(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when (env :nrepl-port)
    (nrepl/start {:bind (env :nrepl-bind)
                  :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))


(defn start-leftover-states
  "For initially stopped defstates. When a ns with defstate(s) not required through app main ns
   it requires manual starting. e.g. segmentum.api.event"
  []
  (->> @@#'mount/meta-state
    (filter (fn [m] ((:status (val m)) :stopped)))
    (map (fn [[_ state]] (:var state)))
    (apply mount/start)
    :started))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))


(defn start-app [args]
  (doseq [component (-> args
                      (parse-opts cli-options)
                      mount/start-with-args
                      :started
                      (concat (start-leftover-states)))]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))


(defn restart [& states]
  (apply mount/stop states)
  (apply mount/start states))


(defn -main [& args]
  (TimeZone/setDefault (TimeZone/getTimeZone "UTC"))
  (mount/start #'segmentum.config/env)
  (cond
    (nil? (:database-url env))
    (do
      (log/error "Database configuration not found, :database-url environment variable must be set before running")
      (System/exit 1))

    (some #{"init"} args)
    (do
      (migrations/init (select-keys env [:database-url :init-script]))
      (System/exit 0))

    (migrations/migration? args)
    (do
      (migrations/migrate args (select-keys env [:database-url]))
      (System/exit 0))

    :else
    (do
      ;; TODO we are going to enable this when we have the domain model
      (mig/start-migration (:database-url env))
      (start-app args))))


(comment
  (-main)
  (restart))