(ns yamper.view.playlist
  (:require
   [ant-man.core :as ant]
   [cljs.core.async :refer [<!] :refer-macros [go]]
   [cljs.core.match :refer-macros [match]]
   cljsjs.howler
   [reagent.core :as r]
   [yamper.net :as net]
   [yamper.view.common :refer [error]]
   [yamper.utils :refer [format]]))

(defn playlist-comp [app-state]
  (r/with-let [tracks (r/cursor app-state [:playlist :tracks])
               row-handler (fn [_ row-idx]
                             #js{:onDoubleClick
                                 (fn [event]
                                   (go
                                     (let [{item-disk-path :path disk :disk} (@tracks row-idx)]
                                       (match (<! (net/get-file disk item-disk-path))
                                         [:ok url] (js/Howl. (js-obj
                                                              "src" url
                                                              "format" (array "mp4")
                                                              "autoplay" true
                                                              "html5" true
                                                              "onloaderror" (fn [_ err]
                                                                              (error err))
                                                              "onplayerror" (fn [_ err]
                                                                              (error err))))
                                         [:error err] (error err)))))})]
    [ant/layout-content
     [ant/table
      {:columns [{:key "status" :dataIndex "status" :width "5%"}
                 {:title "Name" :key "name" :dataIndex "title" :width "90%"}
                 {:title "Duration" :key "duration" :dataIndex "duration" :width "5%"}]
       :dataSource @tracks
       :onRow row-handler
       :pagination false
       :size "small"}]]))
