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
      (swap! login-data assoc :client-id (str (uuid/make-random-uuid)))))

(defn make-websocket-url [url]
  (str "ws://" (.-host (.-location js/window)) url))

(defmulti execute (fn [command & args] command))

(defmethod execute "set_token" [_ token]
  (swap! login-data assoc :token token))

(defmethod execute "login" [_ status]
  (utils/say "login status: " status))

(defmethod execute :default [command & args]
  (utils/say "unknown command: " command))

(defn start []
  (let [stop-chan (chan)]
    (go
      (let [{:keys [ws-channel error]} (<! (ws-ch (make-websocket-url "/websocket") {:format :str}))]
        (if-not error
          (do
            (>! ws-channel (str "login " (utils/to-json-string {:id (ensure-client-id)
                                                                :token (or (:token @login-data "UNKNOWN"))})))
            (go-loop [{:keys [message]} (<! ws-channel)]
              (utils/say "Got message from server: " (pr-str message))
              (apply execute (string/split message #"\s+"))
              (>! ws-channel (str "ack " message))
              (recur (<! ws-channel))))
          (utils/say "Error: " (pr-str error)))))
    stop-chan))
