(ns clj-fitbit.core-test
  (:use clojure.test
        [oauth.client :as oauth]
        clj-fitbit.core))

(def not-nil? (complement nil?))

(println "To test Fitbit API calls remove \"(System/exit 0)\" in the core_test.clj. Running these tests multiple times will likely exhaust your Fitbit API quota.")

(System/exit 0)

(def oauth-conf (get-oauth-conf "oauth.yaml"))
(def consumer (oauth/make-consumer (:consumer_key oauth-conf)
                (:consumer_secret oauth-conf)
                "http://api.fitbit.com/oauth/request_token"
                "http://api.fitbit.com/oauth/access_token"
                "http://www.fitbit.com/oauth/authorize"
                :hmac-sha1 ))

(def oauth-token (get-token consumer "token.yaml"))

(deftest authorize-oauth
  (testing "Fails if not authorized"
    (with-oauth consumer (:oauth_token oauth-token) (:oauth_token_secret oauth-token)
      (:dupa (get-my-user-info)))))

(with-oauth consumer (:oauth_token oauth-token) (:oauth_token_secret oauth-token)
  (deftest get-user-info-test
    (testing "get-my-user-info"
      (is (not-empty (:user (get-my-user-info)))))
    (testing "get-user-info"
      (is (not-empty (:user (get-user-info "246Q22"))))))

  (deftest get-body-measurements-test
    (testing "get-body-measurements"
      (is (not-empty (:body (get-body-measurements "246Q22" "2013-06-01")))))
    (testing "get-my-body-measurements"
      (is (not-empty (:body (get-my-body-measurements "2013-06-01"))))))

  (deftest get-body-weight-test
    (testing "get-body-weight"
      (is (not-empty (:weight (get-body-weight "246Q22" "2013-06-07")))))
    (testing "get-body-weight-period"
      (is (not-empty (:weight (get-body-weight "246Q22" "2013-06-07" "2013-06-14")))))
    (testing "get-my-body-weight"
      (is (not-nil? (:weight (get-my-body-weight "2013-06-01")))))
    (testing "get-my-body-weight-period"
      (is (not-nil? (:weight (get-my-body-weight "2013-06-01" "2013-06-07"))))))

  (deftest get-body-weight-goal-test
    (testing "get-my-body-weight-goal"
      (is (not-nil? (:goal (get-my-body-weight-goal))))))

  (deftest get-body-fat-test
    (testing "get-body-fat"
      (is (not-nil? (:fat (get-body-fat "246Q22" "2013-06-07")))))
    (testing "get-body-fat period"
      (is (not-nil? (:fat (get-body-fat "246Q22" "2013-06-07" "2013-06-14")))))
    (testing "get-my-body-fat"
      (is (not-nil? (:fat (get-my-body-fat "2013-06-07")))))
    (testing "get-my-body-fat period"
      (is (not-nil? (:fat (get-my-body-fat "2013-06-07" "2013-06-14"))))))

  (deftest get-activities-test
    (testing "get-activities"
      (is (not-nil? (:activities (get-activities "246Q22" "2013-06-07")))))
    (testing "get-my-activities"
      (is (not-nil? (:activities (get-my-activities "2013-06-07"))))))

  (deftest get-activities-goals-test
    (testing "get-my-activities-daily-goals"
      (is (not-nil? (:goals (get-my-activities-daily-goals)))))
    (testing "get-my-activities-weekly-goals"
      (is (not-nil? (:goals (get-my-activities-weekly-goals))))))

  (deftest get-foods-test
    (testing "get-foods"
      (is (not-nil? (:foods (get-foods "246Q22" "2013-06-07")))))
    (testing "get-my-foods"
      (is (not-nil? (:foods (get-my-foods "2013-06-07"))))))

  (deftest get-water-test
    (testing "get-water"
      (is (not-nil? (:water (get-water "246Q22" "2013-06-07")))))
    (testing "get-my-water"
      (is (not-nil? (:water (get-my-water "2013-06-07"))))))

  (deftest get-food-goals-test
    (testing "get-my-food-goals"
      (:goals (get-my-food-goals)))) ; this will be nil

  (deftest get-sleep-test
    (testing "get-sleep"
      (is (not-nil? (:sleep (get-sleep "246Q22" "2013-06-07")))))
    (testing "get-my-sleep"
      (is (not-nil? (:sleep (get-my-sleep "2013-06-07"))))))

  (deftest get-heart-rate-test
    (testing "get-my-heart-rate"
      (is (not-nil? (get-my-heart-rate "2013-06-07")))))

  (deftest get-blood-pressure-test
    (testing "get-my-blood-pressure"
      (is (not-nil? (get-my-blood-pressure "2013-06-07")))))

  (deftest get-glucose-test
    (testing "get-my-glucose"
      (is (not-nil? (get-my-glucose "2013-06-07"))))))