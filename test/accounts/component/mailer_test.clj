(ns accounts.component.mailer-test
  (:require [clojure.test :refer :all]
            [accounts.component.mailer :refer :all]
            [clojure.core.async :refer [<!!]]))

(deftest a-test
  (testing "stubbing out mail"
    (let [mailer (stub-mailer)]
      (mail mailer "me" "hi there" "stocking")
      (is (= (<!! (:channel mailer))
             {:to "me"
              :subject "hi there"
              :body "stocking"})))))
