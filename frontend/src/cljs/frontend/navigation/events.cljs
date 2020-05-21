(ns frontend.navigation.events
  (:require
   [re-frame.core :refer [reg-event-db]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))


(reg-event-db
 ::set-active-panel
 (fn-traced [db [_ active-panel]]
   (assoc db :active-panel active-panel)))
