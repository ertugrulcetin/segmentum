(ns frontend.register.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::register
  (fn [db]
    (:register db)))

(reg-sub
  ::user-form
  ::<- [::register]
  (fn [register]
    (:user-form register)))
