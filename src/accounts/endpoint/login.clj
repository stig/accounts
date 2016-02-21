(ns accounts.endpoint.login
  (:require [accounts.component
             [mailer :refer [mail]]
             [users :refer [find-by-email find-by-id]]]
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

(defn- login-failed
  [reason]
  (layout/base
   (list [:h1 "Login failed"]
         [:p reason]
         [:p "Do you want to try " [:a {:src "/login"} "logging in"] " again?"])))

(defn- current-timestamp []
  (System/currentTimeMillis))

(defn- good-timestamp? [ts]
  ;; timestamps are in milliseconds. Let's expire after 10 minutes.
  (let [max (current-timestamp)
        min (- max (* 10 60 1000))]
    (< min (Long/parseLong ts) max)))

(defn- login-complete
  [users id ts hmac]
  (if (good-timestamp? ts)
    (if-let [user (find-by-id users id)]
      (layout/base
       (list [:h1 "You are logged in!"]))
      (login-failed "No user was found with that user id."))
    (login-failed "Unfortunately the login link you used is not valid at this time.")))

(defn- build-url
  [scheme host user-id timestamp last-login]
  (format "%s://%s/login/%s/%s/%s"
          (name scheme)
          host
          user-id
          timestamp
          (hmac "server-secret" scheme host user-id last-login timestamp)))

(defn- handle-login
  [users email mailer scheme host]
  (if-let [user (find-by-email users email)]
                   (let [timestamp (current-timestamp)
                         user-id (:id user)
                         last-login (:last-login user)]
                     (send-login-email mailer email
                                       (build-url scheme host user-id timestamp last-login))
                     (login-form-success))
                   (login-failed "I'm afraid a user with that email address could not be found in our database.")))

(defn login-endpoint [{users :users
                       mailer :mailer}]
  (context "/login" []
           (POST "/" [email :as {{host "host"} :headers
                                 scheme :scheme}]
                 (handle-login users email mailer scheme host))

           (GET ["/:id/:ts/:hmac" :ts #"[0-9]+"] [id ts hmac]
                (login-complete users id ts hmac))

           (GET "/" [] (login-form))))
