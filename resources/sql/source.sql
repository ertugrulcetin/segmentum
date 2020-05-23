-- :name get-source-by-write-key :? :1
-- :doc retrieves a source by given write key
SELECT * FROM source
WHERE write_key = :write_key;