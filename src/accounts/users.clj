(ns accounts.users
  (:require [clojure.java.jdbc :as j]))

(def ^:private last-insert-rowid (keyword "last_insert_rowid()"))

(defn add
  "Add a user to the db"
  [{spec :spec} user]
  (some-> (j/insert! spec :users user)
          first
          last-insert-rowid))

(defn find-by-id
  "find a user by their id"
  [{spec :spec} id]
  (j/query spec
           ["select * from users where id = ?" id]
           :result-set-fn first))

(defn find-by-email
  "Find user by their email"
  [{spec :spec} email]
  (j/query spec
           ["select * from users where email = ?" email]
           :result-set-fn first))
