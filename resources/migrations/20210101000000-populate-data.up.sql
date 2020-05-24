INSERT INTO roles (id, name, priority)
VALUES ('820f4d99-4a3d-4a10-9128-157457cb5faf', 'admin', 1);
--;;

INSERT INTO users (id, name, surname, email, password_salt, password, role_id)
VALUES ('5182f20d-c788-47cb-8dec-319eefc8bc36', 'ertu','cetin','ertu.ctn@gmail.com', '2dca9ce3-a4a4-4440-b8d2-d6e8b653ec2e' , 'asjdnasjdnadnak', '820f4d99-4a3d-4a10-9128-157457cb5faf');

--;;

INSERT INTO workspace (id, created_by, name)
VALUES ('dcfdf014-39b3-47f2-9b44-b02f843fafe1', '5182f20d-c788-47cb-8dec-319eefc8bc36', 'my-workspace');

--;;

INSERT INTO source_types (type) VALUES ('javascript');

--;;

INSERT INTO destination_types (type) VALUES ('google-analytics');

--;;

INSERT INTO source (id, workspace_id, created_by, name, write_key, type)
VALUES ('7c1c1a4d-bdfa-4346-9ea9-d90f298824c7', 'dcfdf014-39b3-47f2-9b44-b02f843fafe1', '5182f20d-c788-47cb-8dec-319eefc8bc36', 'webapp-source', '23JsaK92Q_mQmgNVqy6mY2vof5dGY5Um', 'javascript');

--;;

INSERT INTO destination (id, created_by, type, source_id, config, enabled)
VALUES ('b7dc04da-75a1-47b6-b863-2e28edfe6f07', '5182f20d-c788-47cb-8dec-319eefc8bc36', 'google-analytics', '7c1c1a4d-bdfa-4346-9ea9-d90f298824c7', '{"trackingId" : "UA-165839048-1"}', true);