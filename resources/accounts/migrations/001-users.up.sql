CREATE TABLE users (
       id INTEGER PRIMARY KEY,
       email TEXT UNIQUE,
       moniker TEXT NOT NULL);
