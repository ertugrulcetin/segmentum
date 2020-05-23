(ns frontend.navigation.views
  (:require
   [re-frame.core :refer [subscribe]]
   [frontend.navigation.subs :as subs]
   [frontend.login.views :refer [render-login-panel]]
   [frontend.register.views :refer [render-register-panel]]))

(defn- panels [panel-name]
  (case panel-name
    :home-panel [:div "Home"]
    :login-panel [render-login-panel]
    :register-panel [render-register-panel]
    [:<>]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (subscribe [::subs/active-panel])]
    [show-panel @active-panel]))
