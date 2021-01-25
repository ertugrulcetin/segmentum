(ns frontend.navigation.views
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [frontend.navigation.subs :as subs]
   [frontend.login.views :refer [render-login-panel]]
   [frontend.register.views :refer [render-register-panel]]
   [frontend.dashboard.views :refer [render-dashboard-panel]]
   [frontend.util :as util]
   [reagent.core :as r]))

(defn- alert [alert]
  (r/create-class
    {:component-did-mount #(util/sleep (fn [] (dispatch [:close-alert])) 3000)
     :reagent-render      (fn [alert]
                            [:div.seg-alert-box
                             (if (= :success (:type alert))
                               {:class "green accent-4"}
                               {:class "red accent-4"})
                             [:i.small.material-icons.seg-alert-left-icon
                              (if (= :success (:type alert))
                                "check_circle"
                                "warning")]
                             [:div.seg-alert-content
                              [:span (:message alert)]
                              [:p (:desc alert)]]
                             [:i.material-icons.seg-alert-right-icon
                              {:on-click #(dispatch [:close-alert])}
                              "close"]])}))


(defn- panels [panel-name]
  (case panel-name
    :home-panel [:div "Home"]
    :login-panel [render-login-panel]
    :register-panel [render-register-panel]
    :dashboard-panel [render-dashboard-panel]
    [:<>]))

(defn show-panel [panel-name]
  (let [alert-data @(subscribe [::subs/alert])]
    [:<>
     (when (:show? alert-data)
       [alert alert-data])
     [panels panel-name]]))

(defn main-panel []
  (let [active-panel (subscribe [::subs/active-panel])]
    [show-panel @active-panel]))
