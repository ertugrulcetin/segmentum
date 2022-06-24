(ns frontend.i18n
  (:require [frontend.util :as util]
            [frontend.common.subs :as subs]
            [re-frame.core :refer [subscribe]]))

(def dict
  {})

(defn translate
  [content & args]
  (let [language (keyword (or @(subscribe [::subs/language]) "tr"))
        params   (if (> (count args) 1) args (first args))]
    (cond
      (util/not-empty? params) (apply (-> dict content language) params)
      (map? content) (if-let [data (language content)] data "")
      (string? content) content
      :else (get-in dict [content language]))))
