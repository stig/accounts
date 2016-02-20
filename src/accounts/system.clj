(ns accounts.system
  (:require [accounts.component
             [mailer :refer [mailer]]
             [users :refer [users]]]
            [accounts.endpoint
             [login :refer [login-endpoint]]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [duct.component
             [endpoint :refer [endpoint-component]]
             [handler :refer [handler-component]]
             [hikaricp :refer [hikaricp]]
             [ragtime :refer [ragtime]]]
            [duct.middleware
             [not-found :refer [wrap-not-found]]
             [route-aliases :refer [wrap-route-aliases]]]
            [meta-merge.core :refer [meta-merge]]
            [ring.component.jetty :refer [jetty-server]]
            [ring.middleware
             [defaults :refer [site-defaults wrap-defaults]]
             [webjars :refer [wrap-webjars]]]))

(def base-config
  {:app {:middleware [[wrap-not-found :not-found]
                      [wrap-webjars]
                      [wrap-defaults :defaults]
                      [wrap-route-aliases :aliases]]
         :not-found  (io/resource "accounts/errors/404.html")
         :defaults   (meta-merge site-defaults {:static {:resources "accounts/public"}})
         :aliases    {"/" "/index.html"}}
   :ragtime {:resource-path "accounts/migrations"}})

(defn new-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :app  (handler-component (:app config))
         :http (jetty-server (:http config))
         :db   (hikaricp (:db config))
         :mailer (mailer (:smtp config))
         :ragtime (ragtime (:ragtime config))
         :users (users)
         :login (endpoint-component login-endpoint))
        (component/system-using
         {:http [:app]
          :app  [:login]
          :ragtime [:db]
          :users [:db]
          :login [:users :mailer]}))))
