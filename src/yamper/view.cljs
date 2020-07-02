(ns yamper.view
  (:require
   [ant-man.core :as ant]
   [reagent.core :as r]
   [yamper.net :as net]
   [yamper.view.controls :as controls]
   [yamper.view.navigator :as navigator]
   [yamper.view.playlist :as playlist]))

(def ^:private app-state (r/atom {:navigator {:hidden false
                                              :tree-data (mapv
                                                          (fn [[disk-name disk]]
                                                            {:title disk-name
                                                             :path "/"
                                                             :children []
                                                             :disk disk})
                                                          @net/disks-store)}
                                  :playlist {:tracks []
                                             :current-track nil}
                                  :state :pause}))

(defn- app-layout []
  [ant/layout
   [controls/controls-comp]
   [ant/layout
    [navigator/navigator-comp app-state]
    [playlist/playlist-comp app-state]]])

(defn- localfs-layout []
  [ant/upload-dragger
   {:className "uploader"
    :directory true
    :multiple true}
   [:p.ant-upload-drag-icon
    [ant/icon {:type "inbox"}]]
   [:p.ant-upload-text
    "Click or drag files and folders to this area"]])

(defn- init-ui! [layout]
  (r/render
   [layout]
   (.getElementById js/document "app")))

(defn init-app-ui! []
  (init-ui! app-layout))

(defn init-localfs-ui! []
  (init-ui! localfs-layout))
