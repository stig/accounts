(ns accounts.main
  (:gen-class)
  (:require [accounts
             [config :as config]
             [system :refer [new-system]]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [duct.middleware.errors :refer [wrap-hide-errors]]
            [meta-merge.core :refer [meta-merge]]
            [ring.middleware
             [anti-forgery :refer [wrap-anti-forgery]]
             [params :refer [wrap-params]]]))

(def prod-config
  {:app {:middleware     [wrap-params
                          wrap-anti-forgery
                          [wrap-hide-errors :internal-error]]
         :internal-error (io/resource "errors/500.html")}})

(def config
  (meta-merge config/defaults
              config/environ
              prod-config))

(defn -main [& args]
  (let [system (new-system config)]
    (println "Starting HTTP server on port" (-> system :http :port))
    (component/start system)))
