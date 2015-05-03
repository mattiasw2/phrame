(ns ^:figwheel-always phrame.web-frame
    (:require-macros [cljs.core.async.macros :as asyncm :refer [go go-loop]])
    (:require [clojure.string :as string]
              [cljs.core.async :as async :refer (<! >! put! alts! chan)]
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

(defn send-ack! [server-chan command]
  (put! server-chan (str "ack " command)))

(defmulti execute (fn [server-chan owner command & args] command))

(defmethod execute "set_token" [server-chan owner command token]
  (swap! login-data assoc :token token)
  (send-ack! server-chan command))

(defmethod execute "login" [server-chan owner command status]
  (utils/say "login status: " status)
  (send-ack! server-chan command))

(defmethod execute "flip" [server-chan owner command]
  (om/set-state! owner :current-image (om/get-state owner :next-image))
  (send-ack! server-chan command))

(defmethod execute "load" [server-chan owner command url]
  (let [preload-img (.createElement js/document "img")]
    (.setAttribute preload-img "src" url)
    (if (.-complete preload-img)
      (send-ack! server-chan command)
      (.addEventListener preload-img "load" #(send-ack! server-chan command))))
  (om/set-state! owner :next-image url))

(defmethod execute :default [owner command & args]
  (utils/say "unknown command: " command))

(defn handle-commands [server-chan owner]
  (let [stop-chan (om/get-state owner :close-connection)]
    (go-loop [[message chan] (alts! [server-chan stop-chan])]
      (if (= chan server-chan)
        (do
          (utils/say "Got message from server: " (pr-str message))
          (let [{:keys [message]} message]
            (when message
              (apply execute server-chan owner (string/split message #"\s+"))
              (recur (alts! [server-chan stop-chan])))))
        (utils/say "stopped listening for server events")))))

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
      {:close-connection (chan)
       :current-image "img/matrix.gif"})
    om/IWillMount
    (will-mount [_]
      (serve-connection owner))
    om/IWillUnmount
    (will-unmount [_]
      (put! (om/get-state owner :close-connection) "stop"))
    om/IRenderState
    (render-state [_ state]
      (d/img #js {:src (:current-image state)
                  :onClick #(.webkitRequestFullScreen (.getElementById js/document "image"))}))))

(when (.getElementById js/document "image")
  (om/root image-view
           app-state
           {:target (.getElementById js/document "image")}))

