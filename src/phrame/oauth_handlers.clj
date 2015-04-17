(ns phrame.oauth-handlers
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clj-http.client :as http]
            [clj-oauth2.client :as oauth2]
            [ring.util.response :as response]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [GET] :as compojure]
            [ring.adapter.jetty :as jetty]
            [phrame.config :refer [config]]))

(def oauth-params (merge {;; These should be set in the config.edn file
                          :redirect-uri "https://example.com.com/oauth2callback"
                          :client-id "*google-client-id*"
                          :client-secret "*google-client-secret*"
                          :scope ["https://www.googleapis.com/auth/userinfo.email" "https://picasaweb.google.com/data/"]
                          ;; These don't need changes
                          :authorization-uri "https://accounts.google.com/o/oauth2/auth"
                          :access-token-uri "https://accounts.google.com/o/oauth2/token"
                          :access-query-param :access_token
                          :grant-type "authorization_code"
                          :access-type "online"
                          :approval_prompt ""}
                         (:oauth-params config)))

(def auth-request (oauth2/make-auth-request oauth-params))

(defn save-token [req]
  (let [token (oauth2/get-access-token oauth-params
                                       (:params req)
                                       auth-request)
        token-info (:body (http/get "https://www.googleapis.com/oauth2/v1/tokeninfo"
                                    {:query-params {:access_token (:access-token token)}
                                     :as :json}))]
    (with-open [out (io/writer (io/file (:token-directory config "/tmp") (:email token-info)))]
      (pprint {:token token
               :info token-info}
              out))
    (response/redirect (:success-url config "/"))))

(def routes
  (-> (compojure/routes
       (GET "/google-oauth" []
            (response/redirect (:uri auth-request)))
       (GET "/oauth2callback" []
            save-token))
      wrap-keyword-params
      wrap-params))
