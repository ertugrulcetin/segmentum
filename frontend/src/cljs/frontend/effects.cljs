(ns frontend.effects
  (:require [re-frame.core :refer [reg-fx dispatch]]
            [frontend.util :as util]))

(reg-fx
  :set-user!
  (fn [user]
    (util/set-item! "user" user)))

(reg-fx
  :remove-user!
  (fn [k]
    (util/remove-item! k)))

(reg-fx
  :change-uri!
  (fn [uri]
    (util/set-uri-token! uri)))
