# Fitbit client API for Clojure #

Access the Fitbit API from Clojure.

https://wiki.fitbit.com/display/API/Fitbit+API


[![Build Status](https://api.travis-ci.org/dworznik/clj-fitbit.png)](https://travis-ci.org/dworznik/clj-fitbit)


# Example

    (require '[oauth.client :as oauth]
      '[clj-fitbit.core :as fitbit])

    (def oauth-conf (fitbit/get-oauth-conf "oauth.yaml"))
    (def consumer (oauth/make-consumer (:consumer_key oauth-conf)
                    (:consumer_secret oauth-conf)
                    "http://api.fitbit.com/oauth/request_token"
                    "http://api.fitbit.com/oauth/access_token"
                    "http://www.fitbit.com/oauth/authorize"
                    :hmac-sha1 ))
    (def oauth-token (fitbit/get-token consumer "token.yaml"))

    (fitbit/with-oauth consumer (:oauth_token oauth-token) (:oauth_token_secret oauth-token)
      (println (str "Hello " (get-in (fitbit/get-my-user-info) [:user :displayName ]) "!"))
      (if-let [weight (last (:weight (fitbit/get-my-body-weight "2013-06-15")))]
        (println (str "You weighed " (:weight weight) " kg on 2013-06-15"))
        (println "You didn't weigh yourself on 2013-06-15.")))





## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
