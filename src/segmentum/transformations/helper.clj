(ns segmentum.transformations.helper
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [kezban.core :refer :all]))


(defay mappings (-> (io/resource "transforms/mappings.edn")
                  slurp
                  edn/read-string))


(defn data-transform [incoming-data integration-key]
  (loop [mapping @mappings
         transform {}]
    (if (empty? mapping)
      transform
      (let [mapping-data (first mapping)]
        (recur (drop 1 mapping)
          (if-let [value (get-in incoming-data (:old-path mapping-data))]
            (assoc-in transform (-> mapping-data :new-path integration-key) value)
            transform))))))
