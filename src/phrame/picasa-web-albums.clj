(ns phrame.picasa-web-albums
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.instant :as instant]
            [clojure.xml :as xml]
            [clojure.zip :as zip :refer [xml-zip]]
            [clojure.data.zip.xml :refer [xml1-> xml->] :as zip-xml]
            [clj-http.client :as http]))

(def config (edn/read-string (slurp (io/resource "config.edn"))))

(defn load-user [username]
  (edn/read-string (slurp (io/file (:token-directory config) username))))

(defn refresh-token [token]
  (:body (http/post "https://www.googleapis.com/oauth2/v3/token"
                    {:query-params {:client_id (:oauth-client-id config)
                                    :client_secret (:oauth-client-secret config)
                                    :refresh_token (:refresh-token token)
                                    :grant_type "refresh_token"}
                     :as :json})))

(defn token-info [token]
  (:body (http/get "https://www.googleapis.com/oauth2/v1/tokeninfo"
                   {:query-params {:access_token (:access-token token)}
                    :as :json})))

(defn access-token [user]
  (:access_token (refresh-token (:token (load-user user)))))

(defn xml-zip-string [string]
  (-> string
      .getBytes
      java.io.ByteArrayInputStream.
      xml/parse
      xml-zip))

(defn api-get
  ([user url]
   (api-get user url {}))
  ([user url query-params]
   (-> (http/get url
                 {:headers {"GData-Version" "2"
                            "Authorization" (str "Bearer " (access-token user))}
                  :query-params query-params})
       :body
       xml-zip-string)))

(defn ensure-coll [x]
  (if (coll? x)
    x
    [x]))

(defn parse-integer [s]
  (Integer/parseInt s))

(defn xml-to-map [xml key->element]
  (reduce (fn [result [key parser]]
            (let [[accessor converter] (ensure-coll parser)]
              (assoc result
                     key
                     ((or converter identity) (if (keyword? accessor)
                                                (xml1-> xml accessor zip-xml/text)
                                                (accessor xml))))))
          {}
          key->element))

(defn make-album [xml]
  (xml-to-map xml
              {:title :title
               :id :gphoto:id
               :url :id}))

(defn get-albums [user]
  (map make-album (xml-> (api-get user "https://picasaweb.google.com/data/feed/api/user/default")
                         :entry)))

(defn jpeg-link [xml]
  (xml1-> xml
          :media:group
          :media:content
          (zip-xml/attr :url)))

(defn make-photo [xml]
  (xml-to-map xml
              {:published [:published instant/read-instant-timestamp]
               :updated [:updated instant/read-instant-timestamp]
               :title :title
               :width [:gphoto:width parse-integer]
               :height [:gphoto:height parse-integer]
               :size [:gphoto:size parse-integer]
               :url jpeg-link}))

(defn get-images [user album]
  (map make-photo (xml-> (api-get user
                                  (str "https://picasaweb.google.com/data/feed/api/user/default/albumid/"
                                       (:id album))
                                  {"imgmax" "d"})
                         :entry)))
