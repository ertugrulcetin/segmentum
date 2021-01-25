(ns frontend.common.helper)

(defn show-alert
  [db {:keys [type message desc] :or {type :success}}]
  (assoc db :alert {:type    type
                    :message message
                    :desc    desc
                    :show?   true}))
