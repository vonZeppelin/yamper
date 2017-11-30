(ns yamper.net
  (:require
    [cljs.core.async :as async]
    [alandipert.storage-atom :as storage]
    [cemerick.url :refer [url url-decode]]
    [cljs-http.client :as http]
    [cognitect.transit :as transit])
  (:require-macros
    [cljs.core.async.macros :as async])
  (:import
    goog.history.Html5History))

(defprotocol IDisk
  (disk-name [this]))

(def ^:private ydisk-api-url (url "https://cloud-api.yandex.net/v1/disk/"))

(defn- ydisk-auth-params [token]
  {:with-credentials? false
   :headers {"Authorization" (str "OAuth " token)}})

(defrecord ^:private YandexDisk [token]
  IDisk
  (disk-name [_]
    (async/go
     (let [response (async/<! (http/get ydisk-api-url (ydisk-auth-params token)))
           {:keys [success body error-text error-code]} response]
       (if success
         (-> body :user :login)
         (ex-info error-text {:error error-code}))))))

(defn ydisk-oauth-redirect! []
  (let [client-id "717ab2f77f24432b8ebde8bc736e64e0"
        query-params {:response_type "token"
                      :client_id client-id
                      :display "popup"}
        yandex-authorize-url (url "https://oauth.yandex.com/authorize")
        redirect-url (assoc yandex-authorize-url :query query-params)]
    (.. js/window -location (assign redirect-url))))

(defn register-ydisk []
  (let [hash (.. js/window -location -hash)
        clear-hash! (fn []
                      (if (.isSupported Html5History)
                        (doto (Html5History.)
                          (.setUseFragment false)
                          (.replaceToken ""))
                        (set! (.. js/window -location -hash) "")))
        decode-match (fn [match]
                       (map url-decode (rest match)))
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
