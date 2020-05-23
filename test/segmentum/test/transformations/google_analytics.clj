(ns segmentum.test.transformations.google-analytics
  (:require
   [clojure.test :refer :all]
   [segmentum.test.helper :as test-helper]
   [segmentum.transformations.helper :as helper]
   [clojure.edn :as edn]
   [clojure.java.io :as io]))



(deftest segmentum-input-data-test
  (let [input      (test-helper/json-file->clj-data "ga_input.json")
        output     (test-helper/json-file->clj-data "ga_output.json")
        transform  (helper/data-transform (first input) :ga)]
    (is (= (first output) transform))))
