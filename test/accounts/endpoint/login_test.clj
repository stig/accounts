(ns accounts.endpoint.login-test
  (:require [clojure.test :refer :all]
            [accounts.endpoint.login :as login]
            [com.stuartsierra.component :as component]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]))

(def handler
  (login/login-endpoint {}))

(deftest smoke-test
  (testing "login page exists"
    (-> (session handler)
        (visit "/login")
        (has (status? 200) "page exists"))))
