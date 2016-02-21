(ns accounts.component.users-test
  (:require [accounts.component.users :refer :all]
            [accounts.main :refer [config]]
            [clojure.test :refer :all]))

(def db-spec {:classname   "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname     "db/dev.sqlite"})

(deftest users-test
  (testing "find non-existing user"
    (is (nil? (find-by-email db-spec (gensym))))
    (is (nil? (find-by-id db-spec (gensym)))))

  (testing "adding user"
    (is (number? (add db-spec {:email (gensym)
                               :moniker (gensym)}))))

  (testing "retrieving added user"
    (let [email (str (gensym))
          id (add db-spec {:email email :moniker "Known Name"})
          user {:id id :email email :moniker "Known Name"}]

      (is (= user (find-by-email db-spec email)))
      (is (= user (find-by-id db-spec id))))
    )
  )
