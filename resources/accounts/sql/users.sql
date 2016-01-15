-- name: insert-user<!
-- Insert a user
INSERT INTO users (email, moniker)
VALUES (LOWER(:email), :moniker)

-- name: find-user-by-id
-- Find a user by their user id.
SELECT *
FROM users
WHERE id = ?

-- name: find-user-by-email
-- Find a user by their email address.
SELECT *
FROM users
WHERE email = LOWER(?)

