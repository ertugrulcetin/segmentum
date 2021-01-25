(ns frontend.common.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx]]
   [frontend.db :as db]
   [frontend.effects]
   [day8.re-frame.http-fx]
   [frontend.common.helper :as helper]
   [frontend.i18n :refer [translate]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [frontend.util :as util]))

(reg-event-fx
  :initialize-db
  [(inject-cofx :current-user)]
  (fn [{:keys [_ current-user]}]
    (let [db (assoc db/default-db :current-user current-user)]
      {:db db})))


(reg-event-db
  :no-http-on-ok
  (fn [db _]
    (helper/show-alert db {:type    :error
                           :message "Error"
                           :desc    "Error"})))  ;TODO Translate

(reg-event-db
  :no-http-on-failure
  (fn [db [_ err]]
    (helper/show-alert db {:type    :error
                           :message "Error"
                           :desc    "Error"}))) ;TODO Translate

(reg-event-db
  :reset-in
  (fn [db [_ ks]]
    (util/dissoc-in db ks)))

(reg-event-db
  :reset
  (fn [db [_ k]]
    (dissoc db k)))

(reg-event-db
  :add-data
  (fn [db [_ path value]]
    (assoc-in db path value)))

(reg-event-db
  :show-alert
  (fn [db [_ data]]
    (helper/show-alert db {:type    (:type data)
                           :message (translate (:message data))
                           :desc    (translate (:desc data))})))

(reg-event-db
  :close-alert
  (fn [db _]
    (assoc-in db [:alert :show?] false)))
