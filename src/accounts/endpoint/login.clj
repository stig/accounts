(ns accounts.endpoint.login
  (:require [accounts
             [layout :as layout]
             [users :as users]]
            [accounts.component.mailer :refer [mail]]
            [compojure.core :refer :all]
            [hiccup
             [form :refer :all]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn- login-form []
  (layout/base
   (list
    [:h1 "Login"]
    (form-to [:post "/login"]
             (anti-forgery-field)
             (label :email "Email:")
             (email-field :email)
             (submit-button :submit)))))



(defn- send-login-email [mailer email]
  (mail mailer email "One-time login URL"
        (str "Please click the below link to continue logging in:\n\n"
             (format "http://0.0.0.0:3000/login/%s/%s/%s"
                     email
                     123
                     "DEADBEEF"))))

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

(defn login-endpoint [{{spec :spec} :db
                       mailer :mailer}]
  (context "/login" []
           (POST "/" [email]
                 (if-let [user (users/find-by-email spec email)]
                   (do
                     (send-login-email mailer email)
                     (login-form-success))
                   (login-form-not-found)))

           (GET "/:email/:ts/:hmac" [email ts hmac]
                (login-confirmation-form email ts hmac))

           (GET "/" [] (login-form))))
