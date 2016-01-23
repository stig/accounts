(ns accounts.users-test
  (:require [accounts.main :refer [config]]
            [accounts.users :refer :all]
            [clojure.test :refer :all]))

(def db {:spec {:classname   "org.sqlite.JDBC"
                :subprotocol "sqlite"
                :subname     "db/dev.sqlite"}})

(deftest users-test
  (testing "find non-existing user"
    (is (nil? (find-by-email db (gensym))))
    (is (nil? (find-by-id db (gensym)))))

  (testing "adding user"
    (is (number? (add db {:email (gensym)
                          :moniker (gensym)}))))

  (testing "retrieving added user"
    (let [email (str (gensym))
          id (add db {:email email :moniker "Known Name"})
          user {:id id :email email :moniker "Known Name"}]

      (is (= user (find-by-email db email)))
      (is (= user (find-by-id db id))))
    )
  )
