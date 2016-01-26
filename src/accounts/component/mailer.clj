(ns accounts.component.mailer
  (:require [com.stuartsierra.component :as component]))

(defrecord Mailer []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn mailer []
  (->Mailer))
