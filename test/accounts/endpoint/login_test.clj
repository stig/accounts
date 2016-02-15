(ns accounts.endpoint.login-test
  (:require [accounts
             [system :refer [new-system]]
             [users :refer [add]]]
            [accounts.component.mailer :refer [stub-mailer]]
            [clojure.core.async :refer [<!! >!!]]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [duct.component.ragtime :refer [migrate]]
            [kerodon
             [core :refer :all]
             [test :refer :all]]))

(defn test-system [config]
  (-> (new-system config)

      ;; We don't want to start an actual server, so let's remove that
      (dissoc :http)

      ;; Don't actually send email: use our test mailer instead
      (assoc :mailer (stub-mailer))))

(def config {:db {:uri "jdbc:sqlite::memory:"}})

(def system (test-system config))

(defn handler [] (some-> system :app :handler))

(defn my-fixture [f]
  (alter-var-root #'system (fn [x] (component/start x)))
  (migrate (:ragtime system))
  (f)
  (alter-var-root #'system (fn [x] (component/stop x))))

(use-fixtures :once my-fixture)

(deftest smoke-test
  (testing "login page exists"
    (-> (session (handler))
        (visit "/login")
        (has (status? 200) "page exists")))

  (testing "bad email not found"
    (-> (session (handler))
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
      (-> (session (handler))
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
                            (<!! channel))))))))
