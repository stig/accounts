(ns accounts.component.mailer
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [>!! chan close!]]
            [postal.core :refer [send-message]]))

(defprotocol Mailer
  (mail [mail-service to subject body]))

(defrecord SmtpMailer [smtp-config]
  Mailer
  component/Lifecycle
  (start [this] this)
  (stop [this] this)
  (mail [{smtp-config :smtp-config} to subject body]
    (send-message smtp-config
                  {:from "accounts@superloopy.io"
                   :to to
                   :subject subject
                   :body body})))

(defn mailer [smtp-config]
  (->SmtpMailer smtp-config))

(defrecord StubMailer [channel]
  Mailer
  component/Lifecycle
  (start [this]
    (assoc this :channel (chan 32)))

  (stop [this]
    (close! (:channel this))
    (dissoc this :channel))

  (mail [_ to subject body]
    (>!! channel {:to to :subject subject :body body})))

(defn stub-mailer []
  (->StubMailer (chan 32)))
