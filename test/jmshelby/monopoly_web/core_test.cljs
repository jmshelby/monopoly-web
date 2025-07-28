(ns jmshelby.monopoly-web.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [jmshelby.monopoly-web.core :as core]))

(deftest sample-test
  (testing "Basic functionality works"
    (is (= 1 1) "Math works")))

(deftest config-test
  (testing "App config is available"
    (is (some? core/debug?) "Debug config exists")))