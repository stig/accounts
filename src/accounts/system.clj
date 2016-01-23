(ns accounts.system
  (:require [accounts.endpoint
             [example :refer [example-endpoint]]
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
         :ragtime (ragtime (:ragtime config))
         :login (endpoint-component login-endpoint)
         :example (endpoint-component example-endpoint))
        (component/system-using
         {:http [:app]
          :app  [:example :login]
          :ragtime [:db]
          :login [:db]
          :example [:db]}))))
