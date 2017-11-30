(ns yamper.view
  (:require
    cljsjs.bootstrap-notify
    cljsjs.bootstrap-treeview
    [yamper.net :as net]
    [yamper.utils :as utils]
    [reagent.core :as reagent]
    [baking-soda.bootstrap3 :as bstrap]))

(defn- notify [msg type]
  (.notify js/$ #js {:message msg} #js {:type type}))

(defn success [msg]
  (notify msg "success"))

(defn error [msg]
  (notify
    (if (utils/error? msg)
      (.-message msg)
      msg)
    "danger"))

(defn add-disk []
  [bstrap/Button
    {:bs-size "small"
     :bs-style "default"
     :on-click net/ydisk-oauth-redirect!}
    [bstrap/Glyphicon {:glyph "plus"}]
    "Add YDisk..."])

(defn disks-tree [disks-store]
  (let [tree-data (clj->js {:data [{:text "Node 1"} {:text "Node 2"}]})
        did-mount-fn (fn [this]
                       (-> this reagent/dom-node js/$ (.treeview tree-data)))]
    (reagent/create-class {:reagent-render (constantly [:div])
                           :component-did-mount did-mount-fn})))

(defn init-ui! [disks-store]
  (reagent/render [add-disk] (.getElementById js/document "add-disk"))
  (reagent/render [disks-tree disks-store] (.getElementById js/document "disks-tree")))
