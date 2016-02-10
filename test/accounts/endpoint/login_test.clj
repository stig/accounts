(ns accounts.endpoint.login-test
  (:require [accounts
             [system :refer [base-config]]
             [users :refer [add]]]
            [accounts.component.mailer :refer [stub-mailer]]
            [accounts.endpoint.login :refer :all]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [duct.component
             [endpoint :refer [endpoint-component]]
             [hikaricp :refer [hikaricp]]
             [ragtime :refer [migrate ragtime]]]
            [kerodon
             [core :refer :all]
             [test :refer :all]]
            [meta-merge.core :refer [meta-merge]]))

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
  (let [system (component/start (test-system config))
        handler (some-> system :login :routes)]
    (try
      (migrate (:ragtime system))

      (testing "login page exists"
        (-> (session handler)
            (visit "/login")
            (has (status? 200) "page exists")))

      (testing "bad email not found"
        (-> (session handler)
            (visit "/login")
            (fill-in "Email:" "notvalid@example.com")
            (press "submit")
            (has (some-text? "User not found")
                 "Couldn't find non-existing user. Phew!")))

      (testing "login success"
        ;; We don't support registering yet, so manually add a test user
        ;; before the login attempt.
        (let [email (str (gensym) "@example.com")
              db-spec (-> system :db :spec)
              user-id (add db-spec {:email email :moniker email})]
          (-> (session handler)
              (visit "/login")
              debug ;; #((prn %) %)
              ;;              (prn email)
                                        ;              prn
              (fill-in "Email:" email)
              (press "submit")
                                        ;             prn
              (has (some-text? "Login token on its way!")
                   "Successful message displayed on page"))))

      (finally (component/stop system)))))
