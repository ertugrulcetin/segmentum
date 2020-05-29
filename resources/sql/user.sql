-- :name create-user! :insert :raw
-- :doc creates a new user record
INSERT INTO users (name, surname, email, password_salt, password, role_id)
VALUES (:name, :surname, :email, :password_salt, :password, :role_id)

-- :name get-user :? :1
-- :doc retrieves a user record with email and password
SELECT * FROM users
WHERE email = :email AND password = :password
