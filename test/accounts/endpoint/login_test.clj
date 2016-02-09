(ns accounts.endpoint.login-test
  (:require [accounts.component.mailer :refer [stub-mailer]]
            [accounts.endpoint.login :refer :all]
            [accounts.system :refer [base-config]]
            [clojure.test :refer :all]
            [meta-merge.core :refer [meta-merge]]
            [com.stuartsierra.component :as component]
            [duct.component
             [endpoint :refer [endpoint-component]]
             [hikaricp :refer [hikaricp]]
             [ragtime :refer [migrate ragtime]]]
            [kerodon
             [core :refer :all]
             [test :refer :all]]
            [clojure.pprint :refer [pprint]]))

(def config {:db {:uri "jdbc:sqlite::memory:"}})

(defn test-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :ragtime (ragtime (:ragtime config))
         :login (endpoint-component login-endpoint)
         :db (hikaricp (:db config))
         :mailer (stub-mailer))
        (component/system-using
         {:login [:db :mailer]
          :ragtime [:db]}))))

(deftest smoke-test
  (testing "login page exists"
    (let [system (component/start (test-system config))
          handler (some-> system :login :routes)]
      (try
        (migrate (:ragtime system))
        (-> (session handler)
            (visit "/login")
            (has (status? 200) "page exists"))
        (finally (component/stop system))))))
