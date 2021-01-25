(ns frontend.validation
  (:require [clojure.string :as str]
            [frontend.util :as util]))

(def url-regex #"^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")

(def email-regex #"(([^<>()\[\]\\.,;:\s@\"]+(\.[^<>()\[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))")

(defn email? [email]
  (boolean
    (and (util/not-blank? email)
      (re-matches email-regex email))))
