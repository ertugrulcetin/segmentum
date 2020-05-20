(ns frontend.navigation.views
  (:require
   [re-frame.core :as re-frame]
   [frontend.navigation.subs :as subs]))

(defn- panels [panel-name]
  (case panel-name
    [:div "Home"]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [show-panel @active-panel]))
