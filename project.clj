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
                 [figwheel "0.2.5-SNAPSHOT"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-time "0.9.0"]
                 [clj-http "1.1.0"]
                 [http-kit "2.1.16"]
                 [sudharsh/clj-oauth2 "0.5.3"]
                 [ring "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [ring-middleware-format "0.5.0"]
                 [liberator "0.12.2"]
                 [compojure "1.3.3"]
                 [org.apache.httpcomponents/httpclient "4.3.5"]
                 [alandipert/enduro "1.2.0"]
                 [com.datomic/datomic-free "0.9.5153"]
                 [im.chit/adi "0.3.1-SNAPSHOT"]

                 ;; ClojureScript dependencies
                 [jarohen/chord "0.6.0"]
                 [org.clojure/clojurescript "0.0-2850"]
                 [org.omcljs/om "0.8.8"]
                 [racehub/om-bootstrap "0.5.0"]
                 [alandipert/storage-atom "1.2.4"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.1"]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-figwheel "0.2.5-SNAPSHOT"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"]

  :source-paths ["src/clojure"]

  :cljsbuild {
              :builds [{:id "web-frame-dev"
                        :source-paths ["src/cljs" "dev_src"]
                        :compiler {:output-to "resources/public/js/compiled/web-frame.js"
                                   :output-dir "resources/public/js/compiled/out-web-frame"
                                   :optimizations :none
                                   :main phrame.web-frame-dev
                                   :asset-path "js/compiled/out-web-frame"
                                   :source-map true
                                   :source-map-timestamp true
                                   :cache-analysis true}}
                       {:id "web-frame-min"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/web-frame.js"
                                   :main phrame.web-frame
                                   :optimizations :advanced
                                   :pretty-print false}}
                       {:id "cms-dev"
                        :source-paths ["src/cljs" "dev_src"]
                        :compiler {:output-to "resources/public/js/compiled/cms.js"
                                   :output-dir "resources/public/js/compiled/out-cms"
                                   :optimizations :none
                                   :main phrame.cms-dev
                                   :asset-path "js/compiled/out-cms"
                                   :source-map true
                                   :source-map-timestamp true
                                   :cache-analysis true}}
                       {:id "cms-min"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/phrame.js"
                                   :main phrame.cms
                                   :optimizations :advanced
                                   :pretty-print false}}]}

  :figwheel {
             :http-server-root "public" ;; default and assumes "resources" 
             :server-port 3449 ;; default
             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             :nrepl-port 7888

             :ring-handler phrame.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"
             })
