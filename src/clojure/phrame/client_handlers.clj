(ns phrame.client-handlers
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [>!! <!!]]
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
    (http-server/send! (:channel client) command)
    (<!! (:ack client))))

(defn get-imgmax [width]
  (or (and (number? width)
           (first (drop-while (partial > width)
                              [94 110 128 200 220 288 320 400 512 576 640 720 800 912 1024 1152 1280 1440 1600])))
      "d"))

(defn load-album [client]
  (let [{:keys [owner album]} (:phrame client)
        {:keys [aspect-ratio width]} (:screen client)
        matching-ratio? (if aspect-ratio
                          (if (pos? (- aspect-ratio 1)) pos? neg?)
                          identity)]
    (shuffle (filter (fn [{:keys [width height]}]
                       (matching-ratio? (- (/ width height) 1)))
                     (picasa-web-albums/get-images owner album :imgmax (get-imgmax width))))))

(defn next-picture [client]
  (let [[picture & more-pictures] (or (:playlist client)
                                      (load-album client))]
    (send-client! client "load" (:url picture))
    (send-client! client "flip")
    (let [agent *agent*]
      (timer/schedule-task 30000
                           (send-off agent next-picture)))
    (assoc client :playlist more-pictures)))

(defn make-screen-spec [{:keys [width height]}]
  (when (and width height)
    {:width width
     :height height
     :aspect-ratio (/ width height)}))

(defn client-login [client {:keys [id token screen]}]
  (let [phrame (storage/get-frame id)
        channel (:channel client)]
    (println "token:" token "expected:" (:token phrame) "screen:" screen)
    (condp = token
      nil
      (do (println "no token sent")
          (send-client! client "login" "denied")
          (http-server/close channel)
          client)

      "UNKNOWN"
      (let [token (uuid)]
        (println "new phrame:" id)
        (storage/update-frame id {:token token})
        (send-client! client "set_token" token)
        (send-client! client "login" "accepted")
        client)

      (:token phrame)
      (do (send-client! client "login accepted")
          (println "phrame" id "logged in")
          (next-picture (assoc client
                               :phrame phrame
                               :screen (make-screen-spec screen))))

      (do (if phrame
            (println "phrame" id "sent invalid token")
            (println "phrame" id "is unknown"))
          (send-client! client "login" "denied")
          (http-server/close channel)
          client))))

(defn handle-client-message [channel message]
  (let [[command arg] (string/split message #"\s+" 2)
        client (@clients channel)]
    (condp = command
      "ack" (>!! (:ack @client) arg)
      "login" (send-off client client-login (json/read-str arg :key-fn keyword))
      (println "command " command " not matched:" message))))

(defn websocket-handler [request]
  (http-server/with-channel request channel
    (println "connection established")
    (swap! clients assoc channel (agent {:channel channel
                                         :ack (async/chan)}))
    (http-server/on-close channel
                          (fn [status]
                            (println "channel closed:" status)
                            (swap! clients dissoc channel)))
    (http-server/on-receive channel
                            (partial handle-client-message channel))))

(def routes
  (compojure/routes
   (GET "/websocket" [] websocket-handler)))
