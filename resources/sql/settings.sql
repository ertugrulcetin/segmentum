-- :name get-settings :? :*
-- :doc retrieves all settings
SELECT * FROM settings;


-- :name get-setting :? :1
-- :doc retrieves a setting record given the key
SELECT s.value FROM settings AS s
WHERE key = :key;


-- :name set-setting! :! :1
-- :doc creates a setting with given key and value. If exists, updates it.
INSERT INTO settings (key, value)
VALUES (:key, :value)
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;