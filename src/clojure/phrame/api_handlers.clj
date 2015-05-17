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
                         (context "/:id" [id]
                                  (GET "/" [] (storage/get-frame id))
                                  (PUT "/" {body :body} (storage/upsert-frame id body))))
                (context "/user" []
                         (GET "/" [] (storage/get-users))
                         (context "/:id" [email]
                                  (GET "/" [] (storage/get-user email))
                                  (PUT "/" {body :body} (storage/upsert-user email))))))
      (wrap-restful-format :formats [:json-kw :edn :yaml-kw :yaml-in-html :transit-json :transit-msgpack])))

