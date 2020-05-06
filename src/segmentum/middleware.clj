(ns segmentum.middleware
  (:require
   [segmentum.env :refer [defaults]]
   [cheshire.generate :as cheshire]
   [cognitect.transit :as transit]
   [clojure.tools.logging :as log]
   [segmentum.layout :refer [error-page]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [muuntaja.middleware :refer [wrap-format wrap-params]]
   [segmentum.config :refer [env]]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))


(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status  500
                     :title   "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))


(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
    (wrap-defaults
      (-> site-defaults
        (assoc-in [:security :anti-forgery] false)
        (assoc-in [:session :store] (ttl-memory-store (* 60 30)))))
    wrap-params
    wrap-internal-error))
