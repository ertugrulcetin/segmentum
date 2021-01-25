(ns frontend.util
  (:require [clojure.string :as str]
            [ajax.core :as ajax]
            [pushy.core :as pushy]
            [cemerick.url :as url]
            [re-frame.core :as rf :refer [subscribe dispatch]]
            [lambdaisland.uri :refer [uri]]
            [secretary.core :as secretary]))

(goog-define api-url "http://localhost:3000")

(def not-nil? (complement nil?))

(def not-empty? (complement empty?))

(def not-blank? (complement str/blank?))

(def history (pushy/pushy secretary/dispatch!
               #(when (secretary/locate-route (-> % uri :path)) %)))

(defn sleep
  [f ms]
  (js/setTimeout f ms))

(defn remove-items!
  [keys]
  (try
    (doseq [k keys]
      (.removeItem (.-localStorage js/window) k))
    (catch js/Error e
      (println e))))

(defn remove-item!
  [key]
  (remove-items! [key]))

(defn set-item!
  [key val]
  (try
    (.setItem (.-localStorage js/window) key (.stringify js/JSON (clj->js val)))
    (catch js/Error e
      (println e)
      (remove-item! key))))

(defn create-request-map
  ([type uri]
   (create-request-map type uri nil nil))
  ([type uri on-success]
   (create-request-map type uri on-success nil))
  ([type uri on-success on-fail]
   (cond-> {:headers         {"Authorization" (str "Token token=\"" @(subscribe [:token]) "\"")}
            :method          type
            :uri             (str api-url uri)
            :format          (ajax/json-request-format)
            :response-format (ajax/json-response-format {:keywords? true})
            :on-success      (if (vector? on-success) on-success [on-success])
            :on-failure      (if (vector? on-fail) on-fail [on-fail])}
     (nil? on-success) (assoc :on-success [:no-http-on-ok])
     (nil? on-fail) (assoc :on-failure [:no-http-on-failure]))))

(defn dissoc-in
  ([m ks]
   (if-let [[k & ks] (seq ks)]
     (if (seq ks)
       (let [v (dissoc-in (get m k) ks)]
         (if (empty? v)
           (dissoc m k)
           (assoc m k v)))
       (dissoc m k))
     m))
  ([m ks & kss]
   (if-let [[ks' & kss] (seq kss)]
     (recur (dissoc-in m ks) ks' kss)
     (dissoc-in m ks))))

(defn window-origin
  []
  (if-let [origin (-> js/window .-location .-origin)]
    origin
    (str (-> js/window .-location .-protocol) "//"
      (-> js/window .-location .-hostname)
      (if-let [port (-> js/window .-location .-port)]
        (str ":" port)
        ""))))

(defn set-uri-token! [uri]
  (let [u (url/url (window-origin))
        k (str u uri)]
    (pushy/set-token! history k)
    (secretary/dispatch! uri)))

(defn get-current-user-map []
  (try
    (into (sorted-map)
      (as-> (.getItem js/localStorage "user") data
        (.parse js/JSON data)
        (js->clj data :keywordize-keys true)))
    (catch js/Error _
      {})))

(defn get-url-params []
  (->> js/window.location.href
    (re-seq #"[?&]+([^=&]+)=([^&]*)")
    (map (fn [[_ k v]]
           [(keyword k) v]))
    (into {})))

(defn dispatch-n
  [& args]
  (doseq [k args]
    (dispatch (if (vector? k) k (vector k)))))
