(ns phrame.server
  (:require [org.httpkit.server :as http-server]
            [compojure.core :as compojure]
            [phrame client-handlers oauth-handlers]))

(defonce server (atom nil))

(def handler
  (compojure/routes phrame.client-handlers/routes
                    phrame.oauth-handlers/routes))

(defn start-server []
  (when @server
    (@server))
  (reset! server
          (http-server/run-server handler {:port 9090})))
