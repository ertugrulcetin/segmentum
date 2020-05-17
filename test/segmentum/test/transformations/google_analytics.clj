(ns segmentum.test.transformations.google-analytics
  (:require
   [clojure.test :refer :all]
   [segmentum.test.helper :as test-helper]))


(deftest segmentum-input-data-test
  (testing "should"
    (let [input-json (test-helper/json-file->clj-data "ga_input.json")
          data       (test-helper/clj->json (first input-json))]
      (is 1 1))))
