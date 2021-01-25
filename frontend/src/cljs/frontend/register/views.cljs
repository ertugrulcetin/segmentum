(ns frontend.register.views
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [frontend.register.subs :as subs]
   [reagent.core :as r]))


(defn input [id label-text type path value]
  [:div.input-field
   [:input
    {:id id
     :type type
     :value value
     :on-change #(dispatch [:add-data path (.. % -target -value)])}]
   [:label {:for id} label-text]])


(defn render-register-panel []
  (r/create-class
    {:reagent-render (fn []
                       (let [user-form @(subscribe [::subs/user-form])]
                         [:div.seg-full-container.seg-cc-container
                          [:div.row
                           [input "name" "Name" "text"
                            [:register :user-form :name]
                            (:name user-form)]]
                          [:div.row
                           [input "surname" "Surname" "text"
                            [:register :user-form :surname]
                            (:surname user-form)]]
                          [:div.row
                           [input "email" "Email" "text"
                            [:register :user-form :email]
                            (:email user-form)]]
                          [:div.row
                           [input "password" "Password" "password"
                            [:register :user-form :password]
                            (:password user-form)]]
                          [:div.row
                           [input "confirm-password" "Confirm Password" "password"
                            [:register :user-form :confirm-password]
                            (:confirm-password user-form)]]
                          [:div.row
                           [:button.btn.waves-effect.waves-light
                            {:on-click #(dispatch [:create-user])}
                            "Register"
                            [:i.material-icons.right "send"]]]]))}))
