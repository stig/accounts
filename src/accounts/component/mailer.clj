(ns accounts.component.mailer
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [>!! chan]]
            [postal.core :refer [send-message]]))

(defprotocol Mailer
  (mail [mail-service to subject body]))

(defrecord SmtpMailer [smtp-config]
  Mailer
  (mail [this to subject body]
    (send-message (:smtp-config this)
                  {:from "accounts@superloopy.io"
                   :to to
                   :subject subject
                   :body body})))

(defn mailer [smtp-config]
  (->SmtpMailer smtp-config))

(defrecord StubMailer [channel]
  Mailer
  (mail [_ to subject body]
    (>!! channel {:to to :subject subject :body body})))

(defn stub-mailer []
  (->StubMailer (chan 32)))

