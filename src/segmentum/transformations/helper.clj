(ns segmentum.transformations.helper)


(defn data-transform [mappings-data incoming-data integration-key]
  (loop [mapping mappings-data
         transform {}]
    (if (empty? mapping)
      transform
      (let [mapping-data (first mapping)]
        (recur (drop 1 mapping)
          (if-let [value (get-in incoming-data (:old-path mapping-data))]
            (assoc-in transform (-> mapping-data :new-path integration-key) value)
            transform))))))
