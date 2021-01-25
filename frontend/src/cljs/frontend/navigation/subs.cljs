(ns frontend.navigation.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::active-panel
  (fn [db _]
    (:active-panel db)))

(reg-sub
  ::alert
  (fn [db _]
    (:alert db)))
