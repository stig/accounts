(ns accounts.users
  (:require [clojure.java.jdbc :as j]))

(def ^:private last-insert-rowid (keyword "last_insert_rowid()"))

(defn add
  "Add a user to the db"
  [db-spec user]
  (some-> (j/insert! db-spec :users user)
          first
          last-insert-rowid))

(defn find-by-id
  "find a user by their id"
  [db-spec id]
  (j/query db-spec
           ["select * from users where id = ?" id]
           :result-set-fn first))

(defn find-by-email
  "Find user by their email"
  [db-spec email]
  (j/query db-spec
           ["select * from users where email = ?" email]
           :result-set-fn first))
