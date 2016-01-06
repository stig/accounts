(ns accounts.endpoint.login
  (:require [accounts.layout :as layout]
            [compojure.core :refer :all]
            [hiccup
             [form :refer [email-field form-to submit-button]]
             [page :refer [html5]]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn login-form []
  (list
   [:h1 "Login"]
   (form-to [:post "/login"]
            (anti-forgery-field)
            (email-field :email)
            (submit-button :submit))))


(defn login-endpoint [config]
  (context "/login" []
           (POST "/" [email]
                 (html5 (layout/base
                         {:content
                          (list [:h1 "Email"]
                                [:p email])})))
           (GET "/" []
                (html5 (layout/base
                        {:content (login-form)})))))
