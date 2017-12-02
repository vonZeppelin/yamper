(ns yamper.net
  (:require
    [cljs.core.async :refer [<!]]
    [alandipert.storage-atom :as storage]
    [cemerick.url :as url]
    [cljs-http.client :as http]
    [cognitect.transit :as transit])
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:import
    goog.history.Html5History))

(defprotocol IDisk
  (disk-name [this])
  (get-metadata [this object-path])
  (get-file [this file-path]))

(def ^:private ydisk-api-url (url/url "https://cloud-api.yandex.net/v1/disk/"))

(defrecord ^:private YandexDisk [token]
  IDisk
  (disk-name [_]
    (go
     (let [response (<! (http/get
                          ydisk-api-url
                          {:headers {"Authorization" (str "OAuth " token)}}))
           {:keys [body error-text success]} response]
       (if success
         (-> body :user :login)
         (-> body :message (ex-info {:error error-text}))))))
  (get-metadata [_ object-path]
    (go
      (let [audio-or-dir? (fn [{:keys [media_type type]}]
                            (or
                              (= type "dir")
                              (= media_type "audio")))
            item->node (fn [{:keys [name path type]}]
                         {:children (if (= type "dir")
                                      []
                                      nil)
                          :label name
                          :path path})
            response (<! (http/get
                           (url/url ydisk-api-url "resources")
                           {:headers {"Authorization" (str "OAuth " token)}
                            :query-params {:fields "name,type,path,_embedded.items.media_type,_embedded.items.name,_embedded.items.path,_embedded.items.type"
                                           :limit 2147483647
                                           :path object-path
                                           :sort "name"}}))
            {:keys [body error-text success]} response]
        (if success
          {:children (->> (get-in body [:_embedded :items])
                          (filter audio-or-dir?)
                          (mapv item->node))
           :label (:name body)
           :path (:path body)}
          (-> body :message (ex-info {:error error-text}))))))
  (get-file [_ file-path]
    (go
      nil)))

(defn ydisk-oauth-redirect! []
  (let [query-params {:client_id "717ab2f77f24432b8ebde8bc736e64e0"
                      :display "popup"
                      :force_confirm "yes"
                      :response_type "token"}
        yandex-authorize-url (url/url "https://oauth.yandex.com/authorize")
        redirect-url (assoc yandex-authorize-url :query query-params)]
    (.. js/window -location (assign redirect-url))))

(defn try-register-ydisk []
  (let [hash (.. js/window -location -hash)
        clear-hash! (fn []
                      (if (.isSupported Html5History)
                        (doto (Html5History.)
                          (.setUseFragment false)
                          (.replaceToken ""))
                        (set! (.. js/window -location -hash) "")))
        decode-match (fn [match]
                       (->> match rest (map url/url-decode)))
        process-error (fn [[error error_description]]
                        (throw
                          (ex-info error_description {:error error})))
        register-disk (fn [[access-token]]
                        (->YandexDisk access-token))]
    (when-not (empty? hash)
      (clear-hash!)
      (condp re-find hash
        #"access_token=([^&]+)" :>> (comp register-disk decode-match)
        #"error=([^&]+)&error_description=([^&]+)" :>> (comp process-error decode-match)
        nil))))

(swap!
  storage/transit-write-handlers
  assoc
  YandexDisk
  (transit/write-handler (constantly "ydisk") :token))

(swap!
  storage/transit-read-handlers
  assoc
  "ydisk"
  (transit/read-handler ->YandexDisk))
