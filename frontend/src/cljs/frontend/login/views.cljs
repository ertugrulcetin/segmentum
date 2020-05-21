(ns frontend.login.views
  (:require
   [re-frame.core :refer [dispatch]]
   [frontend.login.subs :as subs]
   [reagent.core :as r]))


(defn input [id label-text type path]
  [:div.input-field
   [:input
    {:id id
     :type type
     :on-change #(dispatch [:add-data path (.. % -target -value)])}]
   [:label {:for id} label-text]])


(defn render-login-panel []
  (r/create-class
    {:reagent-render (fn []
                       [:div.container
                        [:div.row
                         [:div.col
                          [input "email" "Email" "text" [:login :form :email]]]]
                        [:div.row
                         [:div.col
                          [input "password" "Password" "password" [:login :form :password]]]]])}))
