(ns accounts.component.users
  (:require [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as j]))

(defrecord Users []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn users []
  (->Users))

(defn find-by-email
  [{{spec :spec} :db} email]
  (j/query spec
           ["select id from users where email = ?" email]
           :result-set-fn first))
