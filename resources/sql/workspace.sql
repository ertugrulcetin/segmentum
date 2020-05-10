-- :name create-workspace! :insert :raw
-- :doc creates a new workspace record
INSERT INTO workspace (created_by, name)
VALUES (:created_by, :name);


-- :name get-workspaces :? :*
-- :doc retrieves all workspaces
SELECT * FROM workspace;


-- :name get-workspace :? :1
-- :doc retrieves a workspace record with given id
SELECT * FROM workspace
WHERE id = :id;