-- :name get-destinations-by-source-id :? :*
-- :doc retrieves destinations by given source id
SELECT * FROM destination
WHERE source_id = :source_id;