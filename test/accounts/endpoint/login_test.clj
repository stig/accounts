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

(defn- add-user [email]
  (add (-> system :users) {:email email :moniker "Full Name"}))

(defn- future-timestamp []
  (+ (System/currentTimeMillis) 1000))

(defn- good-timestamp []
  (- (System/currentTimeMillis) 1000))

(defn login-failed [session text]
   (within session [:h1]
           (has (text? "Login failed")))
   (within session [:p]
           (has (some-text? text))))

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
        (login-failed "that email address could not be found"))

    (is (nil? (poll! (channel)))))

  (testing "login initiated - email received"
    ;; We don't support registering yet, so manually add a test user
    ;; before the login attempt.
    (let [email (str (gensym) "@example.com")
          user-id (add-user email)]

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

  (testing "login failed - bad timestamps"
    (-> (session (handler))
        (visit "/login/1/666/deadbeef")
        (login-failed "the login link you used is not valid at this time"))

    (-> (session (handler))
        (visit (format "/login/1/%s/deadbeef" (future-timestamp)))
        (login-failed "the login link you used is not valid at this time"))

    (-> (session (handler))
        (visit "/login/1/bad-timestamp/deadbeef")
        (has (status? 404))))

  (testing "login failed - user not found"
    (-> (session (handler))
        (visit (format "/login/666/%s/deadbeef" (good-timestamp)))
        (login-failed "No user was found with that user id.")))

  (testing "login link incorrect shape"
    (-> (session (handler))
        (visit "/login/1")
        (has (status? 404)))

    (-> (session (handler))
        (visit "/login/1/2")
        (has (status? 404)))

    (-> (session (handler))
        (visit "/login/1/2/3/4")
        (has (status? 404)))))
