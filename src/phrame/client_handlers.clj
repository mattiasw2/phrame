(ns phrame.client-handlers
  (:require [org.httpkit.server :as http-server]
            [compojure.core :refer [GET] :as compojure]))

(defonce client (atom nil))

(defn handler [request]
  (http-server/with-channel request channel
    (println "connection established")
    (http-server/on-close channel
                          (fn [status]
                            (println "channel closed: " status)))
    (reset! client channel)
    (http-server/send! channel "load http://upload.wikimedia.org/wikipedia/commons/2/27/IBM-S360-67ConfigurationConsoleCloseup.jpg")
    (http-server/send! channel "flip")))

(def routes
  (compojure/routes
   (GET "/websocket" [] handler)))
