(ns frontend.navigation.events
  (:require
   [re-frame.core :as re-frame]))


(re-frame/reg-event-db
 ::set-active-panel
 (fn-traced [db [_ active-panel]]
   (assoc db :active-panel active-panel)))
