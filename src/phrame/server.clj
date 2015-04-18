(ns phrame.server
  (:require [org.httpkit.server :as http-server]
            [compojure.core :as compojure]
            [phrame.config :refer [config]]
            [phrame.client-handlers]
            [phrame.oauth-handlers]))

(defonce server (atom nil))

(def handler
  (compojure/routes phrame.client-handlers/routes
                    phrame.oauth-handlers/routes))

(defn start-server []
  (when @server
    (@server))
  (reset! server
          (http-server/run-server handler {:port (:port config 9090)})))
