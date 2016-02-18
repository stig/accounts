(ns accounts.endpoint.login-test
  (:require [accounts.component
             [mailer :refer [stub-mailer]]
             [users :refer [add]]]
            [accounts.system :refer [new-system]]
            [clojure.core.async :refer [<!! >!! poll!]]
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

(defn channel []
  (some-> system :mailer :channel))

(defn handler [] (some-> system :app :handler))

(defn my-fixture [f]
  (alter-var-root #'system (fn [x] (component/start x)))
  (migrate (:ragtime system))
  (f)
  (alter-var-root #'system (fn [x] (component/stop x))))

(use-fixtures :once my-fixture)

(defn- find-link [mail-body]
  (re-find #"/login/\d+/\d+/[a-f0-9]+" mail-body))

(deftest smoke-test
  (testing "login page exists"
    (-> (session (handler))
        (visit "/login")
        (has (status? 200) "page exists")))

  (testing "user not found - doesn't send email"
    (-> (session (handler))
        (visit "/login")
        (fill-in "Email:" "notvalid@example.com")
        (press "submit")
        (within [:h1]
                (has (text? "User not found"))))

    (is (nil? (poll! (channel)))))

  (testing "login initiated - email received"
    ;; We don't support registering yet, so manually add a test user
    ;; before the login attempt.
    (let [email (str (gensym) "@example.com")
          channel (-> system :mailer :channel)
          user-id (add users {:email email :moniker email})]

      (-> (session (handler))
          (visit "/login")
          (fill-in "Email:" email)
          (press "submit")
          (within [:h1]
                  (has (text? "Login token on its way!"))))

      ;; User got login message
      (let [m (<!! (channel))]
        (is (= email (:to m)))
        (is (= "One-time login URL" (:subject m)))
        (is (not (nil? (find-link (:body m))))))

      ;; No more messages on the channel
      (is (nil? (poll! (channel))))))

  (testing "login link expired"
    (-> (session (handler))
        (visit "/login/1/666/deadbeef")
        (within [:h1]
                (has (text? "Login link expired"))))))
