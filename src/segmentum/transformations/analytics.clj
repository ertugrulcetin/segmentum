(ns segmentum.transformations.analytics
  (:require [segmentum.transformations.google-analytics :refer [google-analytics-handler]]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))


(defn segmentum-event-params [data]
  "This function should return segmentum event params"
  (select-keys data [:type :category :action :label :value :client-id
                     :anonymize-ip :data-source :user-id :session-control
                     :user-agent :ip-override :geographical :cache-buster
                     :document-referrer :campaign-name :campaign-source
                     :campaign-medium :campaign-keyword :campaign-content
                     :campaign-id :screen-resolution :viewport-size
                     :document-encoding :screen-colors :user-language
                     :java-enabled :flash-version :document-location-url
                     :document-host-name :document-path :document-title
                     :screen-name :content-group :link-id :application-name
                     :application-id :application-version :application-installer-id
                     :transaction-id :transaction-affiliation :transaction-revenue
                     :transaction-shipping :transaction-tax :item-name :item-price
                     :item-quantity :item-code :item-category :transaction-id
                     :affiliation :revenue :tax :shipping :coupon-code
                     :product-action-list :checkout-step :checkout-step-option
                     :currency-code :social-network :social-action
                     :social-action-target :user-timing-category :user-timing-time
                     :user-timing-variable-name :user-timing-label :page-load-time
                     :dns-time :page-download-time :redirect-response-time
                     :tcp-connect-time :server-response-time :dom-interactive-time
                     :content-load-time :exception-description :is-exception-fatal
                     :disabling-advertising-personalization :queue-time]))


(defn segmentum-integration-routes [integration-key params]
  (case integration-key
    :google (google-analytics-handler params)))


(defn segmentum-event-handler [params integration-key]
  (->> params
    (cske/transform-keys csk/->kebab-case)
    segmentum-event-params
    (segmentum-integration-routes integration-key)))


