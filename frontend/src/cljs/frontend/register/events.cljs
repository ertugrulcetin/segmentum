(ns frontend.register.events
  (:require
   [re-frame.core :refer [reg-event-fx reg-event-db]]
   [frontend.validation :as validation]
   [frontend.util :as util]))

(defn- user-form-validation [user-form]
  (and (validation/email? (:email user-form))
    (:name user-form)
    (:surname user-form)
    (= (:password user-form)
      (:confirm-password user-form))))

(reg-event-fx
  :create-user
  (fn [{:keys [db]} _]
    (let [user-form (-> db :register :user-form)]
      (if (user-form-validation user-form)
        {:http-xhrio (merge (util/create-request-map :post "/register")
                       {:params user-form})}
        {:dispatch [:show-alert {:type :error
                                 :message :register/validate-error
                                 :desc :register/validate-error-desc}]}))))
