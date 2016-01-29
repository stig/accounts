(ns accounts.component.mailer-test
  (:require [clojure.test :refer :all]
            [accounts.component.mailer :refer :all]
            [clojure.core.async :refer [<!!]]
            [com.stuartsierra.component :as component]))

(deftest a-test
  (testing "stub-mailer leaves its messages on a channel"
    (let [mailer (component/start (stub-mailer))]
      (try
        (mail mailer "me" "hi there" "stocking")
        (is (= (<!! (:channel mailer))
               {:to "me"
                :subject "hi there"
                :body "stocking"}))
        (finally
          (component/stop mailer))))))
