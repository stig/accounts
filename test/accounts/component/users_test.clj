(ns accounts.component.users-test
  (:require [accounts.component.users :as u]
            [accounts.system :refer [base-config]]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [duct.component
             [hikaricp :refer [hikaricp]]
             [ragtime :refer [ragtime migrate]]]
            [meta-merge.core :refer [meta-merge]]))

(def config {:db {:uri "jdbc:sqlite::memory:"}})

(defn test-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :db (hikaricp (:db config))
         :ragtime (ragtime (:ragtime config))
         :users (u/users))
        (component/system-using
         {:ragtime [:db]
          :users [:db]}))))

(def system (test-system config))

(defn users [] (some-> system :users))

(defn my-fixture [f]
  (alter-var-root #'system (fn [x] (component/start x)))
  (migrate (:ragtime system))
  (f)
  (alter-var-root #'system (fn [x] (component/stop x))))

(use-fixtures :once my-fixture)

(deftest users-test
  (testing "find non-existing user"
    (is (nil? (u/find-by-email (users) (gensym))))
    (is (nil? (u/find-by-id (users) (gensym)))))

  (testing "adding user"
    (is (number? (u/add (users) {:email (gensym)
                                 :moniker (gensym)}))))

  (testing "retrieving added user"
    (let [email (str (gensym))
          id (u/add (users) {:email email :moniker "Known Name"})
          user {:id id :email email :moniker "Known Name"}]

      (is (= user (u/find-by-email (users) email)))
      (is (= user (u/find-by-id (users) id))))
    )
  )
