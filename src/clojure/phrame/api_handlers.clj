(ns phrame.api-handlers
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clj-time.core :as time]
            [compojure.core :refer [GET PUT context] :as compojure]
            [ring.util.response :as response]
            [ring.middleware.format :refer [wrap-restful-format]]
            [phrame.storage :as storage]
            [phrame.config :as config]))

(def routes
  (-> (compojure/routes
       (context "/api" []
                (context "/frame" []
                         (GET "/" [] (storage/get-frames))
                         (context "/:id" [key]
                                  (GET "/" [] (storage/get-frame key))
                                  (PUT "/" {body :body} (storage/upsert-frame key body))))
                (context "/user" []
                         (GET "/" [] (storage/get-users))
                         (context "/:email" [email]
                                  (GET "/" [] (storage/get-user email))
                                  (PUT "/" {body :body} (storage/upsert-user email))))))
      (wrap-restful-format :formats [:json-kw :edn :yaml-kw :yaml-in-html :transit-json :transit-msgpack])))

