(ns segmentum.test.transformations.google-analytics
  (:require
   [clojure.test :refer :all]
   [segmentum.transformations.google-analytics :as google-analytics]))


(deftest google-analytics-event-params-test
  (testing "should return filter data"
    (is
      (google-analytics/google-analytics-params
        {:type "event" :category "test-category" :name "segmentum"})
      {:t "event" :ec "test-category" :name "segmentum"})))
