(ns segmentum.test.handler
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :refer :all]
   [segmentum.handler :refer :all]
   [muuntaja.core :as m]
   [mount.core :as mount]))


(defn parse-json [body]
  #_(m/decode formats/instance "application/json" body))


(use-fixtures
  :once
  (fn [f]
    (mount/start #'segmentum.config/env
      #'segmentum.handler/app-routes)
    (f)))


(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))


  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response))))))
