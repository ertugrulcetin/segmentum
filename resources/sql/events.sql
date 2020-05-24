-- :name create-event! :insert :raw
-- :doc creates a new event record
INSERT INTO events (id, write_key, params, arrived_at)
VALUES (:id, :write_key, :params, :arrived_at);


-- :name create-events! :! :n
-- :doc creates multiple event records
INSERT INTO events (id, write_key, params, arrived_at)
VALUES :tuple*:events;


-- :name create-success-event! :insert :raw
-- :doc creates a new success event record
INSERT INTO success_events (write_key, destination_id, arrived_at, event_id, request_payload, response)
VALUES (:write_key, :destination_id, :arrived_at, :event_id, :request_payload, :response);


-- :name create-fail-event! :insert :raw
-- :doc creates a new failed event record
INSERT INTO fail_events (write_key, destination_id, arrived_at, event_id, request_payload, response, timeout)
VALUES (:write_key, :destination_id, :arrived_at, :event_id, :request_payload, :response, :timeout);