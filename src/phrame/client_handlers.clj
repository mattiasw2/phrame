(ns phrame.client-handlers
  (:require [clojure.string :as string]
            [clj-time.core :as time]
            [org.httpkit.server :as http-server]
            [org.httpkit.timer :as timer]
            [compojure.core :refer [GET] :as compojure]
            [ring.util.response :as response]
            [phrame.storage :as storage]
            [phrame.picasa-web-albums :as picasa-web-albums]))

(defonce clients (atom {}))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn send-client! [client & args]
  (let [command (string/join " " args)]
    (println "sending command" command)
    (http-server/send! (:channel client) command)))

(defn load-album [client]
  (let [{:keys [owner album]} (:phrame client)]
    (shuffle (picasa-web-albums/get-images owner album))))

(defn next-picture [client]
  (let [[picture & more-pictures] (or (:playlist client)
                                      (load-album client))]
    (send-client! client "load" (:url picture))
    (send-client! client "flip")
    (let [agent *agent*]
      (timer/schedule-task 10000
                           (send-off agent next-picture)))
    (assoc client :playlist more-pictures)))

(defmulti client-command (fn [client command & args] command))

(defmethod client-command "login" [client _ id token]
  (let [phrame (get-in @storage/data [:phrames id])
        channel (:channel client)]
    (println "token:" token "expected:" (:token phrame))
    (condp = token
      "UNKNOWN"
      (let [token (uuid)]
        (println "new phrame:" id)
        (storage/swap! assoc-in [:phrames id] {:token token})
        (http-server/send! channel (str "set_token " token))
        (http-server/send! channel "login accepted")
        client)

      (:token phrame)
      (do (http-server/send! channel "login accepted")
          (println "phrame" id "logged in")
          (next-picture (assoc client :phrame phrame)))

      (do (if phrame
            (println "phrame" id "sent invalid token")
            (println "phrame" id "is unknown"))
          (http-server/send! channel "login denied")
          (http-server/close channel)
          client))))

(defmethod client-command "ack" [client _]
  client)

(defn handle-client-command [client command]
  (println "client command" command)
  (apply client-command client (string/split command #"\s+")))

(defn websocket-handler [request]
  (http-server/with-channel request channel
    (println "connection established")
    (swap! clients assoc channel (agent {:channel channel}))
    (http-server/on-close channel
                          (fn [status]
                            (println "channel closed:" status)
                            (swap! clients dissoc channel)))
    (http-server/on-receive channel
                            (partial send-off (@clients channel) handle-client-command))))

(def routes
  (compojure/routes
   (GET "/websocket" []
        websocket-handler)))
