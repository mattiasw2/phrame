(ns ^:figwheel-always phrame.cms
    (:require [om.core :as om :include-macros true]
              [om-tools.dom :as d :include-macros true]
              [om-bootstrap.button :as b]
              [om-bootstrap.nav :as n]
              [om-bootstrap.random :as r]
              [om-bootstrap.input :as i]
              [om-bootstrap.table :refer [table]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn navbar []
  (n/navbar
   {:brand (d/a {:href "#"}
                "Phrame")}
   (n/nav
    {:collapsible? true}
    (n/nav-item {:key 1 :href "#albums"} "Albums")
    (n/nav-item {:key 2 :href "#frames"} "Frames"))))

(defn main-view [data owner]
   (reify om/IRender
     (render [_]
       (println "rendering")
       (d/div
        (navbar)
        (d/div {:class "container"}
               (d/img {:id "image"
                       :src "img/matrix.gif"}))))))

(om/root main-view
         app-state
         {:target (. js/document (getElementById "app"))})


