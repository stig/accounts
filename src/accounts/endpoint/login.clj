(ns accounts.endpoint.login
  (:require [accounts.layout :as layout]
            [compojure.core :refer :all]
            [hiccup.page :refer [html5]]))

(defn login-endpoint [config]
  (context "/login" []
           (GET "/" []
                (html5 (layout/base {:content [:p "Hello from Login Page"]})))))
