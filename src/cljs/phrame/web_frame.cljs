(ns ^:figwheel-always phrame.web-frame
    (:require-macros [cljs.core.async.macros :as asyncm :refer [go go-loop]])
    (:require [clojure.string :as string]
              [cljs.core.async :as async :refer (<! >! alts! chan)]
              [chord.client :refer [ws-ch]]
              [alandipert.storage-atom :refer [local-storage]]
              [cljs-uuid-utils.core :as uuid]
              [phrame.utils :as utils]))

(def login-data (local-storage (atom {}) :login-data))

(defn ensure-client-id []
  (or (:client-id @login-data)
      (:client-id (swap! login-data assoc :client-id (str "web:" (uuid/make-random-uuid))))))

(defn make-websocket-url [url]
  (str "ws://" (.-host (.-location js/window)) url))

(defmulti execute (fn [command & args] command))

(defmethod execute "set_token" [_ token]
  (swap! login-data assoc :token token))

(defmethod execute "login" [_ status]
  (utils/say "login status: " status))

(defmethod execute "flip" [_]
  (utils/say "flip is not yet implemented"))

(defmethod execute "load" [_ url]
  (.setAttribute (.getElementById js/document "image") "src" url)
  (utils/say "done"))

(defmethod execute :default [command & args]
  (utils/say "unknown command: " command))

(defn handle-commands [server-chan stop-chan]
  (go-loop [[message chan] (alts! [server-chan stop-chan])]
    (if (= chan server-chan)
      (do
        (utils/say "Got message from server: " (pr-str message))
        (let [{:keys [message]} message]
          (when message
            (apply execute (string/split message #"\s+"))
            (>! server-chan (str "ack " message))
            (recur (alts! [server-chan stop-chan])))))
      (utils/say "server stopped"))))

(defn start []
  (utils/say "starting server")
  (let [stop-chan (chan)]
    (go
      (let [channel-info (<! (ws-ch (make-websocket-url "/websocket") {:format :str}))]
        (if-let [error (:error channel-info)]
          (utils/say "Error: " (pr-str error))
          (let [server-chan (:ws-channel channel-info)]
            (>! server-chan (str "login " (utils/to-json-string {:id (ensure-client-id)
                                                                 :token (or (:token @login-data "UNKNOWN"))
                                                                 :screen {:width (.-width js/screen)
                                                                          :height (.-height js/screen)}})))
            (handle-commands server-chan stop-chan)))))
    stop-chan))

(defonce frame (start))
