(ns segmentum.transformations.google-analytics
  (:require [clojure.set :as set]
            [clj-http.client :as client]))


;;
;; Google Analytics Tracking id
;;


(def google-analytics-version 1)
(def google-tracking-id "UA-165839048-1")
(def h {"User-Agent" "Mozilla/5.0 (Windows NT 6.1;) Gecko/20100101 Firefox/13.0.1"})


(defn send-google-analytics [params]
  (client/post "https://www.google-analytics.com/collect"
    {:headers h
     :form-params params}))


(defn google-analytics-params [params]
  (-> params
    (set/rename-keys {:type :t
                      :category :ec
                      :action :ea
                      :label :el
                      :value :ev
                      :client-id :cid
                      :user-agent :ua
                      :session-control :sc
                      :ip-override :uip
                      :data-source :ds
                      :anonymize-ip :aip
                      :user-id :uid
                      :disabling-advertising-personalization :npa
                      :queue-time :qt
                      :cache-buster :z
                      :geographical :geoid
                      :document-referrer :dr
                      :campaign-name :cn
                      :campaign-source :cs
                      :campaign-medium :cm
                      :campaign-keyword :ck
                      :campaign-content :cc
                      :campaign-id :ci
                      :screen-resolution :sr
                      :viewport-size :vp
                      :document-encoding :de
                      :screen-colors :sd
                      :user-language :ul
                      :java-enabled :je
                      :flash-version :fl
                      :document-location-url :dl
                      :document-host-name :dh
                      :document-path :dp
                      :document-title :dt
                      :screen-name :cd
                      :link-id :linkid
                      :application-name :an
                      :application-id :aid
                      :application-version :av
                      :application-installer-id :aiid
                      :transaction-id :ti
                      :transaction-affiliation :ta
                      :transaction-revenue :tr
                      :transaction-shipping :ts
                      :transaction-tax :tt
                      :item-name :in
                      :item-price :ip
                      :item-quantity :iq
                      :item-code :ic
                      :item-category :iv
                      :affiliation :ta
                      :revenue :tr
                      :tax :tt
                      :shipping :ts
                      :coupon-code :tcc
                      :product-action-list :pal
                      :checkout-step :cos
                      :checkout-step-option :col
                      :currency-code :cu
                      :social-network :sn
                      :social-action :sa
                      :social-action-target :st
                      :user-timing-category :utc
                      :user-timing-time :utt
                      :user-timing-variable-name :utv
                      :user-timing-label :utl
                      :page-load-time :plt
                      :dns-time :dns
                      :page-download-time :pdt
                      :redirect-response-time :rrt
                      :tcp-connect-time :tcp
                      :server-response-time :srt
                      :dom-interactive-time :dit
                      :content-load-time :clt
                      :exception-description :exd
                      :is-exception-fatal :exf})


    (merge {:v google-analytics-version
            :tid google-tracking-id})))


(defn google-analytics-handler [params]
  (let [google-params (google-analytics-params params)]
    (send-google-analytics google-params)))


