(ns phrame.picasa-web-albums
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.zip :as zip :refer [xml-zip]]
            [clojure.data.zip.xml :refer [xml1-> xml->] :as zip-xml]
            [clj-time.format :as time-format]
            [clj-http.client :as http]
            [phrame.storage :as storage]))

(def config (edn/read-string (slurp (io/resource "config.edn"))))

(defn refresh-token [token]
  (:body (http/post "https://www.googleapis.com/oauth2/v3/token"
                    {:query-params {:client_id (-> config :oauth-params :client-id)
                                    :client_secret (-> config :oauth-params :client-secret)
                                    :refresh_token token
                                    :grant_type "refresh_token"}
                     :as :json})))

(defn access-token [user]
  (:access_token (refresh-token (:google-refresh-token user))))

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
  (Long/parseLong s))

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
               :picasa-id [:gphoto:id parse-integer]
               :url :id
               :updated [:updated time-format/parse]}))

(defn get-albums [user]
  (map make-album (xml-> (api-get user "https://picasaweb.google.com/data/feed/api/user/default")
                         :entry)))

(defn jpeg-link [which xml]
  (xml1-> xml
          :media:group
          which
          (zip-xml/attr :url)))

(defn make-photo [xml]
  (xml-to-map xml
              {:picasa-id [:gphoto:id parse-integer]
               :published [:published time-format/parse]
               :updated [:updated time-format/parse]
               :title :title
               :width [:gphoto:width parse-integer]
               :height [:gphoto:height parse-integer]
               :size [:gphoto:size parse-integer]
               :url (partial jpeg-link :media:content)
               :thumbnail-url (partial jpeg-link :media:thumbnail)}))

(defn get-images [user album
                  & {:keys [imgmax thumbsize] :or {imgmax "d" thumbsize 200}}]
  (assert user)
  (assert album)
  (map make-photo (xml-> (api-get user
                                  (str "https://picasaweb.google.com/data/feed/api/user/default/albumid/"
                                       (:picasa-id album))
                                  {"imgmax" imgmax
                                   "thumbsize" thumbsize})
                         :entry)))

(defn map-by [key coll]
  (into {} (map (fn [element] [(key element) element]) coll)))

(defn synchronize-album-images [user album]
  (let [storage-images (map-by :picasa-id (:images (storage/get-album (:picasa-id album))))
        picasa-images (map-by :picasa-id (get-images user album))]
    (doseq [picasa-id (set/difference (set (keys storage-images)) (set (keys picasa-images)))]
      (println picasa-id "deleted image")
      (storage/delete-image picasa-id))

    (doseq [picasa-id (keys picasa-images)]
      (when (or (nil? (storage-images picasa-id))
                (not= (:updated (storage-images picasa-id)) (:updated (picasa-images picasa-id))))
        (println picasa-id "updated image" (:updated (storage-images picasa-id)) (:updated (picasa-images picasa-id)))
        (storage/upsert-album (:picasa-id album) {:images (:db/id (storage/upsert-image picasa-id (picasa-images picasa-id)))})))))

(defn new-picasa-album [user album]
  (println (:picasa-id album) "new picasa album" (:title album))
  (let [album (storage/upsert-album (:picasa-id album) album)
        user (storage/upsert-user (:email user) {:albums #{[:picasa-album/picasa-id (:picasa-id album)]}})]
    (synchronize-album-images user album)))

(defn update-picasa-album [user album]
  (println (:picasa-id album) "update picasa album")
  (storage/upsert-album (:picasa-id album) (dissoc album :images))
  (synchronize-album-images user album))

(defn delete-picasa-album [album-id]
  (println album-id "delete picasa album")
  (storage/delete-album album-id))

(defn synchronize-albums [user]
  (let [storage-albums (map-by :picasa-id (map #(storage/get-entity (:db/id %)) (:albums user)))
        picasa-albums (map-by :picasa-id (get-albums user))
        all-album-ids (set/union (set (keys storage-albums)) (set (keys picasa-albums)))]
    (doseq [album-id all-album-ids]
      (cond
        (not (storage-albums album-id))
        (new-picasa-album user (picasa-albums album-id))

        (not (picasa-albums album-id))
        (delete-picasa-album album-id)

        (not (= (:updated (storage-albums album-id)) (:updated (picasa-albums album-id))))
        (update-picasa-album user (picasa-albums album-id))))))

#_
(spit "/tmp/data.edn"
      (with-out-str (binding [*print-length* nil *print-level* nil]
                      (clojure.pprint/pprint (take 5 (map #(storage/get-album (:picasa-id %)) (:albums (storage/get-user "hans.huebner@gmail.com"))))))))
