(ns accounts.config
  (:require [environ.core :refer [env]]))

(def defaults
  ^:displace {:http {:port 3000}})

(def environ
  {:http {:port (some-> env :port Integer.)}
   :login-config {:server-secret (some-> env :server-secret)
                  :token-timeout-ms (some-> env :token-timeout-ms)}
   :db   {:uri  (env :database-url)}})
