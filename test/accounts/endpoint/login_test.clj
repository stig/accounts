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
             [handler :refer [handler-component]]
             [hikaricp :refer [hikaricp]]
             [ragtime :refer [migrate ragtime]]]
            [kerodon
             [core :refer :all]
             [test :refer :all]]
            [meta-merge.core :refer [meta-merge]]
            [clojure.core.async :refer [<!! >!!]]))

(def config {:db {:uri "jdbc:sqlite::memory:"}})

(defn test-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :ragtime (ragtime (:ragtime config))
         :login (endpoint-component login-endpoint)
         :db (hikaricp (:db config))
         :mailer (stub-mailer)
         :app (handler-component (:app config)))
        (component/system-using
         {:login [:db :mailer]
          :app [:login]
          :ragtime [:db]}))))

(deftest smoke-test
  (let [system (component/start (test-system config))
        handler (some-> system :app :handler)]
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
            (within [:h1]
                    (has (text? "User not found")))))

      (testing "login success"
        ;; We don't support registering yet, so manually add a test user
        ;; before the login attempt.
        (let [email (str (gensym) "@example.com")
              db-spec (-> system :db :spec)
              channel (-> system :mailer :channel)
              user-id (add db-spec {:email email :moniker email})]
          (-> (session handler)
              (visit "/login")
              (fill-in "Email:" email)
              (press "submit")
              (within [:h1]
                      (has (text? "Login token on its way!"))))

          (let [m (<!! channel)]
            (is (= email (:to m)))
            (is (= "One-time login URL" (:subject m)))
            ;;; TODO check link in body
            )

          ;; We want to check there's no more messages on the channel, however
          ;; <!! blocks if there's no message, so write a sentinel and make
          ;; sure that's what we get back.
          (let [sentinel (keyword (gensym))]
            (is (= sentinel (do (>!! channel sentinel)
                                (<!! channel)))))))

      (finally (component/stop system)))))
