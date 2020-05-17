(ns segmentum.test.transformations.helper
  (:require
   [clojure.test :refer :all]
   [segmentum.transformations.helper :as helper]))


(deftest data-transform-test
  (are [x y] (= x y)
    {:name "segmentum"} (helper/data-transform
                          [{:old-path [:info :name] :new-path {:qa [:name]}}]
                          {:info {:name "segmentum"}}
                          :qa)
    {:info {:name "segmentum"}} (helper/data-transform
                                  [{:old-path [:data :name] :new-path {:qa [:info :name]}}]
                                  {:data {:name "segmentum"}}
                                  :qa)
    {:event "click"} (helper/data-transform
                       [{:old-path [:segment] :new-path {:qa [:handler]}}
                        {:old-path [:info :data :name] :new-path {:qa [:event]}}]
                       {:info {:data {:name "click"}}}
                       :qa)))
