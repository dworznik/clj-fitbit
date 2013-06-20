(ns clj-fitbit.core
  (:require [clj-http.client :as http]
            [clojure.java.io :as io :only [write]]
            [clojure.data.json :as json]
            [oauth.client :as oauth]
            [clojure.string :as string]
            [clj-yaml.core :as yaml]
            [clj-time.format :as time-fmt :only [parse formatters]])
  (:import [java.net URLEncoder]))

(defn get-oauth-conf [fname]
  (yaml/parse-string (slurp fname)))

(defn authorize [consumer]
  (let [request-token (oauth/request-token consumer)]
    (do (println (oauth/user-approval-uri consumer
                   (:oauth_token request-token)))
      (println "Enter verifier:")
      (let [verifier (read-line)]
        (oauth/access-token consumer
          request-token
          verifier))
      )))

(defn get-token [consumer fname]
  (try (yaml/parse-string (slurp fname))
    (catch Exception e
      (let [token (authorize consumer)]
        (with-open [wrt (io/writer fname)]
          (.write wrt (yaml/generate-string token)))
        token))))

(def ^:dynamic *oauth-consumer* nil)
(def ^:dynamic *oauth-access-token* nil)
(def ^:dynamic *oauth-access-token-secret* nil)
(def ^:dynamic *protocol* "http")
(def uri-base "api.fitbit.com")
(def api-version 1)
(def ^:dynamic *unit-system* :metric )

(defn- auth-string [cred] (reduce str (map (fn [[k v]] (str (name k) "=\"" (URLEncoder/encode (str v) "UTF-8") "\",")) cred)))

(defn- map-replace [m text]
  (reduce
    (fn [acc [k v]] (string/replace acc (str k) (str v)))
    text m))

(defn- in?
  "true if seq contains el"
  [seq el]
  (some #(= el %) seq))

(defmacro with-oauth
  [consumer access-token access-token-secret & body]
  `(binding [*oauth-consumer* ~consumer
             *oauth-access-token* ~access-token
             *oauth-access-token-secret* ~access-token-secret]
     (do
       ~@body)))

(defmacro def-fitbit-resource [res-name fns handler & [doc]]
  `(defn ~(with-meta res-name (assoc (meta res-name) :doc doc))
     ~@(for [[req-method req-url required-params optional-params validation mapping] fns]
         (let [fn-required-params (vec (map #(symbol (name %)) required-params))
               unit-headers {:us "en_US" :uk "en_UK"}]
           `([~@fn-required-params]
              ~validation
              (let [url-params-map# (zipmap ~required-params ~fn-required-params)
                    url-params-map# (into {} (for [[k# v#] url-params-map#] [k# ((k# ~mapping identity) v#)]))
                    req-url# (str *protocol* "://" uri-base "/" api-version (map-replace url-params-map# ~req-url))
                    url-params-keys# (vec (map #(keyword %) (re-seq #"(?<=:)[a-zA-Z\-_0-9]+" ~req-url)))
                    req-params-map# (apply dissoc url-params-map# url-params-keys#)
                    req-params-map# (into {} (for [[k# v#] req-params-map#] [k# ((k# ~mapping identity) v#)]))
                    oauth-creds# (when (and *oauth-consumer* *oauth-access-token*)
                                   (oauth/credentials *oauth-consumer*
                                     *oauth-access-token*
                                     *oauth-access-token-secret*
                                     ~req-method
                                     req-url#
                                     req-params-map#))
                    accept-lang# (*unit-system* ~unit-headers)
                    accept-lang-header# (if-not (nil? accept-lang#) {"Accept-language" accept-lang#})
                    auth-header# {"Authorization" (str "OAuth " (auth-string oauth-creds#))}
                    headers# (merge accept-lang-header# auth-header#)]
                (~handler (~(symbol "http" (name req-method))
                            req-url#
                            {:as :json
                             :query-params req-params-map#
                             :headers headers#}))))))))

(def-fitbit-resource get-user-info
  [[:get "/user/:user/profile.json" [:user ] []]]
  :body "Gets user's profile")

(def get-my-user-info (partial get-user-info "-"))

(defn- validate-date [date]
  (try (time-fmt/parse (time-fmt/formatters :year-month-day ) date) (catch Exception e false)))

(def resources
  [:activities/calories :activities/caloriesBMR :activities/steps :activities/distance :activities/floors :activities/elevation :activities/minutesSedentary :activities/minutesLightlyActive :activities/minutesFairlyActive :activities/minutesVeryActive :activities/activeScore :activities/activityCalories :activities/tracker/calories :activities/tracker/steps :activities/tracker/distance :activities/tracker/floors :activities/tracker/elevation :activities/tracker/minutesSedentary :activities/tracker/minutesLightlyActive :activities/tracker/minutesFairlyActive :activities/tracker/minutesVeryActive :activities/tracker/activeScore :activities/tracker/activityCalories :foods/log/caloriesIn :foods/log/water :sleep/startTime :sleep/timeInBed :sleep/minutesAsleep :sleep/awakeningsCount :sleep/minutesAwake :sleep/minutesToFallAsleep :sleep/minutesAfterWakeup :sleep/efficiency :body/weight :body/bmi :body/fat ])

(defn- validate-resource [resource] (in? resources resource))

(def-fitbit-resource get-body-measurements
  [[:get "/user/:user/body/date/:date.json"
    [:user :date ] []
    {:pre [(validate-date date)]}]]
  :body "Get a summary of a user's body measurements for a given day")

(def get-my-body-measurements (partial get-body-measurements "-"))

(def-fitbit-resource get-body-weight
  [[:get "/user/:user/body/log/weight/date/:date.json"
    [:user :date ] []
    {:pre [(validate-date date)]}]
   [:get "/user/:user/body/log/weight/date/:base-date/:end-date.json"
    [:user :base-date :end-date ] []
    {:pre [(validate-date base-date) (validate-date end-date)]}]]
  :body "Get a list of all user's body weight log entries for a given day")

(def get-my-body-weight (partial get-body-weight "-"))

(def-fitbit-resource get-body-fat
  [[:get "/user/:user/body/log/fat/date/:date.json"
    [:user :date ] []
    {:pre [(validate-date date)]}]
   [:get "/user/:user/body/log/fat/date/:base-date/:end-date.json"
    [:user :base-date :end-date ] []
    {:pre [(validate-date base-date) (validate-date end-date)]}]]
  :body "Get a list of all user's body fat log entries for a given day")

(def get-my-body-fat (partial get-body-fat "-"))

(def-fitbit-resource get-my-body-weight-goal
  [[:get "/user/-/body/log/weight/goal.json" []]]
  :body "Get a user's current weight goal")

(def-fitbit-resource get-my-body-fat-goal
  [[:get "/user/-/body/log/fat/goal.json" []]]
  :body "Get a user's current fat goal")

(def-fitbit-resource get-activities
  [[:get "/user/:user/activities/date/:date.json"
    [:user :date ] []
    {:pre [(validate-date date)]}]]
  :body "Get a summary and list of a user's activities and activity log entries for a given day")

(def get-my-activities (partial get-activities "-"))

(def-fitbit-resource get-my-activities-daily-goals
  [[:get "/user/-/activities/goals/daily.json" [] []]]
  :body "Get a user's current daily activity goals")

(def-fitbit-resource get-my-activities-weekly-goals
  [[:get "/user/-/activities/goals/weekly.json" [] []]]
  :body "Get a user's current weekly activity goals")

(def-fitbit-resource get-foods
  [[:get "/user/:user/foods/log/date/:date.json"
    [:user :date ] []
    {:pre [(validate-date date)]}]]
  :body "Get a summary and list of a user's food log entries for a given day")

(def get-my-foods (partial get-foods "-"))

(def-fitbit-resource get-my-water
  [[:get "/user/-/log/water/date/:date.json"
    [:user :date ] []
    {:pre [(validate-date date)]}]]
  :body "Get a summary and list of a user's water log entries for a given day")

(def-fitbit-resource get-my-food-goals
  [[:get "/user/-/foods/log/goal.json" [] []]]
  :body "Get a user's current daily calorie consumption goal and/or Food Plan")

(def-fitbit-resource get-sleep
  [[:get "/user/:user/sleep/date/:date.json" [:user :date ] []
    {:pre [(validate-date date)]}]]
  :body "Get a summary and list of a user's sleep log entries as well as minute by minute sleep entry data for a given day")

(def get-my-sleep (partial get-sleep "-"))

(def-fitbit-resource get-my-heart-rate
  [[:get "/user/-/heart/date/:date.json" [:date ] []
    {:pre [(validate-date date)]}]]
  :body "Get an average values and a list of user's heart rate log entries for a given day")

(def-fitbit-resource get-my-blood-pressure
  [[:get "/user/-/bp/date/:date.json" [:date ] []
    {:pre [(validate-date date)]}]]
  :body "Get an average value and a list of user's blood pressure log entries for a given day")

(def-fitbit-resource get-my-glucose
  [[:get "/user/-/glucose/date/:date.json" [:date ] []
    {:pre [(validate-date date)]}]]
  :body "Get a list of user's blood glucose and HbA1C measurements for a given day")

(def-fitbit-resource get-time-series
  [[:get "/user/:user/:resource/date/:base-date/:end-date.json" [:user :resource :base-date :end-date ] []
    {:pre [(validate-resource resource) (validate-date base-date)
           (or (validate-date end-date) (in? [:1d, :7d, :30d, :1w, :1m, :3m, :6m, :1y, :max ] end-date))]}
    {:end-date #(if (keyword? %) (name %) %) :resource #(if (keyword? %) (str (namespace %) "/" (name %)) %)}
    ]]
  :body )

(def get-my-time-series (partial get-time-series "-"))

(def-fitbit-resource get-my-recent-activities
  [[:get "/user/-/activities/recent.json" [] []]]
  :body )

(def-fitbit-resource get-my-frequent-activities
  [[:get "/user/-/activities/frequent.json" [] []]]
  :body )

(def-fitbit-resource get-my-favorite-activities
  [[:get "/user/-/activities/favorite.json" [] []]]
  :body )

(def-fitbit-resource get-my-recent-foods
  [[:get "/user/-/foods/log/recent.json" [] []]]
  :body )

(def-fitbit-resource get-my-frequent-foods
  [[:get "/user/-/foods/log/frequent.json" [] []]]
  :body )

(def-fitbit-resource get-my-favorite-foods
  [[:get "/user/-/foods/log/favorite.json" [] []]]
  :body )

(def-fitbit-resource get-my-meals
  [[:get "/user/-/meals.json" [] []]]
  :body )

(def-fitbit-resource get-friends
  [[:get "/user/:user/friends.json" [:user ] []]]
  :body )

(def get-my-friends (partial get-friends "-"))

(def-fitbit-resource get-my-friends-leaderboard
  [[:get "/user/-/friends/leaderboard.json" [] []]]
  :body )

(def-fitbit-resource get-my-invites
  [[:get "/user/-/friends/invitations.json" [] []]]
  :body )

(def-fitbit-resource get-badges
  [[:get "/user/:user/badges.json" [:user ] []]]
  :body )

(def get-my-badges (partial get-badges "-"))

(def-fitbit-resource get-my-devices
  [[:get "/user/-/devices.json" [] []]]
  :body )

(def-fitbit-resource get-my-alarms
  [[:get "/user/-/devices/tracker/:tracker/alarms.json" [:tracker ] []]]
  :body "Gets alarms")

(defn- validate-tracker
  [tracker]
  (or (in? '(:resting :normal :exertive ) tracker) (not (keyword? tracker))))

(defn- map-tracker
  [tracker]
  ({:resting "Resting Heart Rate" :normal "Normal Heart Rate" :exertive "Exertive Heart Rate"} tracker tracker))

(def-fitbit-resource log-heart-rate
  [[:post "/user/-/heart.json" [:tracker :heartRate :date ] []
    {:pre [(validate-tracker tracker) (number? heartRate)]}
    {:tracker map-tracker}]
   [:post "/user/-/heart.json" [:tracker :heartRate :date :time ] []
    {:pre [(validate-tracker tracker) (number? heartRate)]}
    {:tracker map-tracker}]]
  :body )

