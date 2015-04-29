(defproject phrame "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main phrame.server
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-time "0.9.0"]
                 [clj-http "1.1.0"]
                 [http-kit "2.1.16"]
                 [sudharsh/clj-oauth2 "0.5.3"]
                 [ring "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.3.3"]
                 [org.apache.httpcomponents/httpclient "4.3.5"]
                 [alandipert/enduro "1.2.0"]])
