(ns frontend.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import [goog History]
           [goog.history EventType])
  (:require
   [secretary.core :as secretary]
   [goog.events :as gevents]
   [re-frame.core :as re-frame]
   [frontend.navigation.events :as navigation-events]
   [frontend.register.events]))

(defn hook-browser-navigation! []
  (doto (History.)
    (gevents/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
    (re-frame/dispatch [::navigation-events/set-active-panel :home-panel]))


  (defroute "/login" []
    (re-frame/dispatch [::navigation-events/set-active-panel :login-panel]))


  (defroute "/register" []
    (re-frame/dispatch [::navigation-events/set-active-panel :register-panel]))


  (defroute "/dashboard" []
    (re-frame/dispatch [::navigation-events/set-active-panel :dashboard-panel]))

  ;; --------------------
  (hook-browser-navigation!))
