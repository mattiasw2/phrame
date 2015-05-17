(ns phrame.server
  (:require [org.httpkit.server :as http-server]
            [ring.middleware.reload :refer [wrap-reload]]
            [compojure.core :as compojure]
            [phrame.config :refer [config]]
            [compojure.route :as route]
            [phrame.client-handlers]
            [phrame.oauth-handlers]
            [phrame.api-handlers]))

(defonce server (atom nil))

(def handler
  (-> (compojure/routes phrame.client-handlers/routes
                        phrame.oauth-handlers/routes
                        phrame.api-handlers/routes
                        (route/resources "/")
                        (route/not-found "Page not found"))
      wrap-reload))

(defn start-server []
  (when @server
    (@server))
  (reset! server
          (http-server/run-server handler {:port (:port config 9090)})))

(defn -main [& args]
  (start-server))
