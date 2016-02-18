(ns accounts.component.mailer
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [>!! chan close!]]
            [postal.core :refer [send-message]]))

(defmulti mail (fn [x & _] (class x)))

(defrecord SmtpMailer [smtp-config])

(defmethod mail SmtpMailer [{:keys [smtp-config]} to subject body]
  (send-message smtp-config
                {:from "accounts@superloopy.io"
                 :to to
                 :subject subject
                 :body body}))

(defn mailer [smtp-config]
  (->SmtpMailer smtp-config))


(defrecord StubMailer [channel]
  component/Lifecycle
  (start [this]
    (assoc this :channel (chan 32)))
  (stop [this]
    (close! (:channel this))
    (assoc this :channel nil)))

(defmethod mail StubMailer [{:keys [channel]} to subject body]
  (>!! channel {:to to :subject subject :body body}))

(defn stub-mailer []
  (->StubMailer nil))
