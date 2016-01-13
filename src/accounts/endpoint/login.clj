(ns accounts.endpoint.login
  (:require [accounts.layout :as layout]
            [compojure.core :refer :all]
            [hiccup
             [form :refer [email-field form-to submit-button]]
             [page :refer [html5]]]
            [postal.core :refer [send-message]]
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
                 (send-message (:smtp config)
                               {:from "accounts@superloopy.io"
                                :to email
                                :subject "One-Time Login URL"
                                :body [{:type "text/html"
                                        :content (html5
                                                  (layout/base
                                                   {:content
                                                    (form-to [:post (format "http://0.0.0.0:3000/login/%s/%s/%s"
                                                                            email
                                                                            123
                                                                            "DEADBEEF")]
                                                             (submit-button "Log me in!"))}))}]})
                 (html5 (layout/base
                         {:content
                          (list [:h1 "Login token on its way!"]
                                [:p "We just sent a login link to your email
                                address. It should be with you shortly. The
                                link is only valid for 20 minutes, so please
                                check your mail."])})))
           (GET "/:email/:ts/:hmac" [email ts hmac]
                (html5 (layout/base
                        {:content [:dl
                                   [:dt :email]
                                   [:dd email]
                                   [:dt :ts]
                                   [:dd ts]
                                   [:dt :hmac]
                                   [:dd hmac]]})))
           (GET "/" []
                (html5 (layout/base
                        {:content (login-form)})))))
