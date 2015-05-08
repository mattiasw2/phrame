(ns phrame.api-handlers
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clj-time.core :as time]
            [compojure.core :refer [GET] :as compojure]
            [ring.util.response :as response]
            [ring.middleware.format :refer [wrap-restful-format]]
            [phrame.storage :as storage]
            [phrame.config :as config]))

(def routes
  (-> (compojure/routes
       (GET "/api/config" [] (response/response config/config))
       (GET "/api/data" [] (response/response @storage/data)))
      (wrap-restful-format :formats [:json-kw :edn :yaml-kw :yaml-in-html :transit-json :transit-msgpack])))

