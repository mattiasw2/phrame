(ns phrame.server
  (:require [org.httpkit.server :as http-server]))

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

(defonce server (atom nil))

(defn start-server []
  (when @server
    (@server))
  (reset! server
          (http-server/run-server handler {:port 9090})))
