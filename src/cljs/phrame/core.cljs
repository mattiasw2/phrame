(ns ^:figwheel-always phrame.core
    (:require [om.core :as om :include-macros true]
              [om-tools.dom :as d :include-macros true]
              [om-bootstrap.button :as b]
              [om-bootstrap.nav :as n]
              [om-bootstrap.random :as r]
              [om-bootstrap.input :as i]
              [om-bootstrap.table :refer [table]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world MUHAHA!"}))

(defn navbar []
  (n/navbar
   {:brand (d/a {:href "#"}
                "Phrame")}
   (n/nav
    {:collapsible? true}
    (n/nav-item {:key 1 :href "#albums"} "Albums")
    (n/nav-item {:key 2 :href "#frames"} "Frames"))))

(om/root
 (fn [data owner]
   (reify om/IRender
     (render [_]
       (println "rendering")
       (d/div
        (navbar)
        (d/div {:class "container"}
               (d/h1 (:text data))
               (d/form
                (i/input {:type "text" :addon-before "@"})
                (i/input {:type "text" :addon-after ".00"})
                (i/input {:type "text" :addon-before "$" :addon-after ".00"}))
               (table {:striped? true :bordered? true :condensed? true :hover? true}
                      (d/thead
                       (d/tr
                        (d/th "#")
                        (d/th "First Name")
                        (d/th "Last Name")
                        (d/th "Username")))
                      (d/tbody
                       (d/tr
                        (d/td "1")
                        (d/td "Mark")
                        (d/td "Otto")
                        (d/td "@mdo"))
                       (d/tr
                        (d/td "2")
                        (d/td "Jacob")
                        (d/td "Thornton")
                        (d/td "@fat"))
                       (d/tr
                        (d/td "3")
                        (d/td {:col-span 2} "Larry the Bird")
                        (d/td "@twitter")))))))))
 app-state
 {:target (. js/document (getElementById "app"))})

