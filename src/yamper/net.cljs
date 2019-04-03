(ns yamper.net
  (:require
   [alandipert.storage-atom :as storage]
   [cemerick.uri :refer [uri uri-decode]]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!] :refer-macros [go]]
   [cljs.core.match :refer-macros [match]]
   [clojure.string :as str]
   [cognitect.transit :as transit]
   [secretary.core :refer-macros [defroute]]))

(defprotocol IDisk
  (browse [this path])
  (disk-name [this])
  (get-file [this file-path]))

(def ^:private ydisk-api-uri (uri "https://cloud-api.yandex.net/v1/disk/"))

(defrecord YandexDisk [token]
  IDisk
  (browse [this path]
    (go
      (let [resp (<! (http/get
                      (uri ydisk-api-uri "resources")
                      {:query-params {:path path
                                      :fields "name,path,type,media_type,_embedded.items.name,_embedded.items.path,_embedded.items.type,_embedded.items.media_type"
                                      :limit 2147483647
                                      :sort "name"}
                       :headers {"Authorization" (str "OAuth " token)}}))
            create-item (fn create-item [{:keys [name path type media_type]
                                          {items :items} :_embedded}]
                          {:title name
                           :path (str/replace-first path "disk:" "")
                           :type media_type
                           :children (when (= type "dir")
                                       (mapv create-item items))})]
        (match resp
          {:success true :body body} [:ok (create-item body)]
          {:success false :body {:message error}} [:error error]
          {:success false :error-text error} [:error error]
          :else [:error "Unknown error"]))))
  (disk-name [_]
    (go
      (let [resp (<! (http/get
                      ydisk-api-uri
                      {:query-params {:fields "user.login,user.display_name"}
                       :headers {"Authorization" (str "OAuth " token)}}))]
        (match resp
          {:success true :body {:user user}} [:ok (or
                                                   (:display_name user)
                                                   (:login user))]
          {:success false :body {:message error}} [:error error]
          {:success false :error-text error} [:error error]
          :else [:error "Unknown error"]))))
  (get-file [_ path]
    (go
      (let [resp (<! (http/get
                      (uri ydisk-api-uri "resources" "download")
                      {:query-params {:path path
                                      :fields "href"}
                       :headers {"Authorization" (str "OAuth " token)}}))]
        (match resp
          {:success true :body {:href href}} [:ok href]
          {:success false :body {:message error}} [:error error]
          {:success false :error-text error} [:error error]
          :else [:error "Unknown error"])))))

(defonce disk-registrators
  {"Yandex.Disk" (fn []
                   (let [query-params {:client_id "717ab2f77f24432b8ebde8bc736e64e0"
                                       :display "popup"
                                       :force_confirm "yes"
                                       :response_type "token"}
                         auth-url (assoc
                                   (uri "https://oauth.yandex.com/authorize")
                                   :query query-params)]
                     (.open js/window auth-url "_blank" "width=640,height=480")))})

(swap!
 storage/transit-write-handlers
 assoc
 YandexDisk
 (transit/write-handler (constantly "ydisk") :token))

(swap!
 storage/transit-read-handlers
 assoc
 "ydisk" (transit/read-handler ->YandexDisk))

(defonce disks-store
  (storage/local-storage
   (atom (sorted-map))
   :disks))

(defroute ^:private ydisk-auth-success #"/access_token=([^&]+).+" [token]
  (when-some [opener (.-opener js/window)]
    (let [disk (->YandexDisk token)]
      (go
        (match (<! (disk-name disk))
          [:ok name] (do
                       (swap! disks-store assoc name disk)
                       (.yamper.view.success
                        opener
                        (str "New disk registered: " name)))
          [:error error] (.yamper.view.error opener error))
        (.close js/window)))))

(defroute ^:private ydisk-auth-error #"/error=(.+)&error_description=(.+)$" [_ error]
  (when-some [opener (.-opener js/window)]
    (.yamper.view.error opener (uri-decode error))
    (.close js/window)))
