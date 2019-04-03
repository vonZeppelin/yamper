(ns yamper.core
  (:require
   [goog.events :as events]
   [secretary.core :as secretary :refer-macros [defroute]]
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
  (view/init-ui!))
