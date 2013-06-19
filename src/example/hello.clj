(ns example.hello
  (:use [clj-fitbit.core]
        [clj-fitbit.auth]
        [oauth.client :as oauth]
        [clj-time.core :as time :only [now minus plus months weeks days]]
        [clj-time.format :as time-fmt :only [unparse formatters]]))


(def month
  (let [now (time/now)
        today (time-fmt/unparse (time-fmt/formatters :year-month-day ) now)
        month-ago (time-fmt/unparse (time-fmt/formatters :year-month-day ) (minus now (months 1)))]
    [month-ago today]))

(defn -main []
  (def oauth-conf (get-oauth-conf "oauth.yaml"))
  (def consumer (oauth/make-consumer (:consumer_key oauth-conf)
                  (:consumer_secret oauth-conf)
                  "http://api.fitbit.com/oauth/request_token"
                  "http://api.fitbit.com/oauth/access_token"
                  "http://www.fitbit.com/oauth/authorize"
                  :hmac-sha1 ))

  (def oauth-token (get-token consumer "token.yaml"))
  (with-oauth consumer (:oauth_token oauth-token) (:oauth_token_secret oauth-token)
    (println (str "Hello " (get-in (get-my-user-info) [:user :displayName ]) "!"))
    (if-let [last-weight (last (:weight (apply get-my-body-weight month)))]
      (println (str "You weigh " (:weight last-weight) " kg."))
      (println "You haven't weighed yourself recently."))))
