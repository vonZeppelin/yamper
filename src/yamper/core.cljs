(ns yamper.core
  (:require
   [cemerick.uri :refer [uri-decode]]
   [cljs.core.async :refer [<!] :refer-macros [go]]
   [cljs.core.match :refer-macros [match]]
   [goog.events :as events]
   [secretary.core :as secretary :refer-macros [defroute]]
   [yamper.net :as net]
   [yamper.view :as view])
  (:import [goog History]
           [goog.history EventType]))

(defn init! []
  (secretary/set-config! :prefix "#")
  (doto (History.)
    (events/listen
     EventType.NAVIGATE
     #(-> % .-token secretary/dispatch!))
    (.setEnabled true)))

(defroute ^:private index "/" []
  (view/init-app-ui!))

(defroute ^:private ydisk-auth-success #"/access_token=([^&]+).+" [token]
  (when-some [opener (.-opener js/window)]
    (let [disk (net/->YandexDisk token)]
      (go
        (match (<! (net/disk-name disk))
          [:ok name] (do
                       (swap! net/disks-store assoc name disk)
                       (.yamper.view.common.success
                        opener
                        (str "Disk registered: " name)))
          [:error error] (.yamper.view.common.error opener error))
        (.close js/window)))))

(defroute ^:private ydisk-auth-error #"/error=(.+)&error_description=(.+)$" [_ error]
  (when-some [opener (.-opener js/window)]
    (.yamper.view.common.error opener (uri-decode error))
    (.close js/window)))

(defroute ^:private localfs #"/localfs" []
  (view/init-localfs-ui!))
