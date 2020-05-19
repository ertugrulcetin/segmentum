-- :name create-event! :insert :raw
-- :doc creates a new event record
INSERT INTO events (id, write_key, payload, arrived_at)
VALUES (:id, :write_key, :payload, :arrived_at);


-- :name create-events! :! :n
-- :doc creates multiple event records
INSERT INTO events (id, write_key, payload, arrived_at)
VALUES :tuple*:events;