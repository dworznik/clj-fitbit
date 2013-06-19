
(defproject clj-fitbit "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.2"]
                 [org.clojure/data.json "0.2.2"]
                 [clj-oauth "1.4.0"]
                 [clj-yaml "0.4.0"]
                 [clj-time "0.5.1"]]
  :test-selectors {:default (fn [m] (not (or (:integration m) (:regression m))))
                   :integration :integration
                   :regression :regression})
