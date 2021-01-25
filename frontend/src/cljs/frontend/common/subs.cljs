(ns frontend.common.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::language
  (fn [db]
    (:language db)))

(reg-sub
  :token
  (fn [db _]
    (-> db :current-user :token)))
