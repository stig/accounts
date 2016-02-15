(ns accounts.endpoint.login
  (:require [accounts.component
             [mailer :refer [mail]]
             [users :refer [find-by-email]]]
            [accounts.layout :as layout]
            [compojure.core :refer :all]
            [hiccup.form :refer :all]
            [pandect.algo.sha256 :refer [sha256-hmac]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn- hmac [secret & key-parts]
  (let [key (clojure.string/join "$" key-parts)]
    (sha256-hmac key secret)))

(defn- login-form []
  (layout/base
   (list
    [:h1 "Login"]
    (form-to [:post "/login"]
             (anti-forgery-field)
             (label :email "Email:")
             (email-field :email)
             (submit-button :submit)))))

(defn- send-login-email [mailer email url]
  (mail mailer email "One-time login URL"
        (str "Please click the below link to continue logging in:\n\n"
             url)))

(defn- login-form-success []
  (layout/base
   (list [:h1 "Login token on its way!"]
         [:p "We just sent a login link to the email address you specified. It
         should be with you shortly. The link is only valid for 20 minutes, so
         please check your mail."])))



(defn- login-form-not-found []
  (layout/base
   (list [:h1 "User not found"]
         [:p "I'm afraid a user with that email address could not be found in
         our database. Would you like to try again?"])))

(defn- login-confirmation-form
  [email ts hmac]
  (layout/base [:dl
                [:dt :email]
                [:dd email]
                [:dt :ts]
                [:dd ts]
                [:dt :hmac]
                [:dd hmac]]))

(defn login-endpoint [{users :users
                       mailer :mailer}]
  (context "/login" []
           (POST "/" [email :as {{host "host"} :headers
                                 scheme :scheme}]
                 (if-let [user (find-by-email users email)]
                   (let [timestamp (System/currentTimeMillis)
                         user-id (:id user)
                         last-login (:last-login user)]
                     (send-login-email mailer email
                                       (format "%s://%s/login/%s/%s/%s"
                                               (name scheme)
                                               host
                                               user-id
                                               timestamp
                                               (hmac "server-secret" scheme host user-id last-login timestamp)))
                     (login-form-success))
                   (login-form-not-found)))

           (GET "/:email/:ts/:hmac" [email ts hmac]
                (login-confirmation-form email ts hmac))

           (GET "/" [] (login-form))))
