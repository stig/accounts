(ns accounts.users
  (:require [clojure.java.jdbc :as j]))

(defn find-by-email
  [{spec :spec} email]
  (j/query spec
           ["select id from users where email = ?" email]
           :result-set-fn first))
