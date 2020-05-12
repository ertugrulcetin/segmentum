(ns segmentum.test.helper
  (:require [clojure.data.json :as json]))


(defn json-file->clj-data [path]
  (-> "test/segmentum/test/data/"
    (str path)
    slurp
    (json/read-str :key-fn keyword)))

(defn clj->json [data]
  (json/write-str data))
