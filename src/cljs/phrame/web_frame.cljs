(ns ^:figwheel-always phrame.web-frame
    (:require-macros [cljs.core.async.macros :as asyncm :refer [go go-loop]])
    (:require [clojure.string :as string]
              [cljs.core.async :as async :refer (<! >! alts! chan)]
              [om.core :as om :include-macros true]
              [om-tools.dom :as d :include-macros true]
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

(defmulti execute (fn [owner command & args] command))

(defmethod execute "set_token" [owner _ token]
  (swap! login-data assoc :token token))

(defmethod execute "login" [owner _ status]
  (utils/say "login status: " status))

(defmethod execute "flip" [owner _]
  (utils/say "flip is not yet implemented"))

(defmethod execute "load" [owner _ url]
  (om/set-state! owner :current-image url))

(defmethod execute :default [owner command & args]
  (utils/say "unknown command: " command))

(defn handle-commands [server-chan owner]
  (let [stop-chan (om/get-state owner :stop-connection)]
    (go-loop [[message chan] (alts! [server-chan stop-chan])]
      (if (= chan server-chan)
        (do
          (utils/say "Got message from server: " (pr-str message))
          (let [{:keys [message]} message]
            (when message
              (apply execute owner (string/split message #"\s+"))
              (>! server-chan (str "ack " message))
              (recur (alts! [server-chan stop-chan])))))
        (utils/say "server stopped")))))

(defn serve-connection [owner]
  (utils/say "establishing connection to server")
  (go
    (let [channel-info (<! (ws-ch (make-websocket-url "/websocket") {:format :str}))]
      (if-let [error (:error channel-info)]
        (utils/say "Error: " (pr-str error))
        (let [server-chan (:ws-channel channel-info)]
          (>! server-chan (str "login " (utils/to-json-string {:id (ensure-client-id)
                                                               :token (or (:token @login-data "UNKNOWN"))
                                                               :screen {:width (.-width js/screen)
                                                                        :height (.-height js/screen)}})))
          (handle-commands server-chan owner))))))

(def app-state (atom {}))

(defn image-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:stop-connection (chan)
       :current-image "img/matrix.gif"})
    om/IWillMount
    (will-mount [_]
      (serve-connection owner))
    om/IRenderState
    (render-state [_ state]
      (d/img {:src (:current-image state)}))))

(om/root image-view
         app-state
         {:target (. js/document (getElementById "image"))})
