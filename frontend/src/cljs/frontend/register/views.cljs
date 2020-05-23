(ns frontend.register.views
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


(defn render-register-panel []
  (r/create-class
    {:reagent-render (fn []
                       [:div.seg-full-container.seg-cc-container
                        [:div.row
                          [input "name" "Name" "text" [:login :form :name]]]
                        [:div.row
                          [input "email" "Email" "text" [:login :form :email]]]
                        [:div.row
                          [input "password" "Password" "password" [:login :form :password]]]
                        [:div.row
                          [input "confirm-password" "Confirm Password" "password" [:login :form :confirm_password]]]
                        [:div.row
                         [:button.btn.waves-effect.waves-light
                          {:on-click #()}
                          "Register"
                          [:i.material-icons.right "send"]]]])}))
