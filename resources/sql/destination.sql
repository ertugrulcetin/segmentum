-- :name get-destinations-by-source-id :? :*
-- :doc retrieves destinations by given source id
SELECT * FROM destination
WHERE source_id = :source_id;


-- :name get-destinations-type-and-conf :? :*
-- :doc retrieves destinations by given source id
SELECT d.id, d.type, d.config FROM destination AS d
WHERE source_id = :source_id;