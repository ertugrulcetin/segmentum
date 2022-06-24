(ns frontend.dashboard.views
  (:require
   [re-frame.core :refer [dispatch]]
   [reagent.core :as r]))

(defn logo-container []
  [:div.seg-menu-container.seg-logo-container
   [:p "Logo"]])

(defn menu-container [{:keys [content icon-key active]}]
  [:div.seg-menu-container
   {:class (when active "seg-menu-active")}
   [:i.material-icons icon-key]
   [:span content]])

(defn sidenav-view []
  [:div.seg-sidenav-container
   [logo-container]
   [menu-container {:content "Home"
                    :icon-key "home"
                    :active false}]
   [menu-container {:content "Connections"
                    :icon-key "timeline"
                    :active true}]])

(defn navbar-view []
  [:div.seg-navbar-container])

(defn center-content-view []
  [:div.seg-center-container])

(defn content-view []
  [:div.seg-dashboard-content
   [navbar-view]
   [center-content-view]])

(defn render-dashboard-panel []
  (r/create-class
    {:reagent-render (fn []
                       [:div.seg-dashboard-container
                        [sidenav-view]
                        [content-view]])}))

