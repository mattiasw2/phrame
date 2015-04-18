(ns phrame.client-handlers
  (:require [clojure.string :as string]
            [org.httpkit.server :as http-server]
            [compojure.core :refer [GET] :as compojure]
            [ring.util.response :as response]
            [phrame.storage :as storage]))

(defonce client (atom nil))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defmulti client-command (fn [channel command & args] command))

(defmethod client-command "login" [channel _ id token]
  (let [phrame (get-in @storage/data [:phrames id])]
    (println "token:" token "expected:" (:token phrame))
    (condp = token
      "UNKNOWN"
      (let [token (uuid)]
        (println "new phrame:" id)
        (storage/swap! assoc-in [:phrames id] {:token token})
        (http-server/send! channel (str "set_token " token)))

      (:token phrame)
      (println "phrame" id "logged in")

      (if phrame
        (println "phrame" id "sent invalid token")
        (println "phrame" id "is unknown")))))

(defn handle-client-command [channel command]
  (println "client command" command)
  (apply client-command channel (string/split command #"\s+")))

(defn websocket-handler [request]
  (http-server/with-channel request channel
    (println "connection established")
    (http-server/on-close channel
                          (fn [status]
                            (println "channel closed:" status)))
    (http-server/on-receive channel
                            (partial handle-client-command channel))
    (reset! client channel)
    (http-server/send! channel "load http://upload.wikimedia.org/wikipedia/commons/2/27/IBM-S360-67ConfigurationConsoleCloseup.jpg")
    (http-server/send! channel "flip")))

(def routes
  (compojure/routes
   (GET "/websocket" []
        websocket-handler)))
