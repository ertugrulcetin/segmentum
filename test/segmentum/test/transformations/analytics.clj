(ns segmentum.test.transformations.analytics
  (:require
   [clojure.test :refer :all]
   [segmentum.transformations.analytics :as analytics]))

(deftest segmentum-event-params-test
  (testing "should return filter data"
    (is (analytics/segmentum-event-params
          {:type "event" :category "test-category" :name "segmentum"})
        {:type "event" :category "test-category"})))
