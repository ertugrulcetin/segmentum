(ns frontend.common.events
  (:require
   [re-frame.core :refer [reg-event-db]]
   [frontend.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))


(reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))
