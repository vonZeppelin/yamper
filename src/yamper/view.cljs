(ns yamper.view
  (:require
    [clojure.data :refer [diff]]
    cljsjs.bootstrap-notify
    [yamper.net :as net]
    [yamper.utils :as utils]
    [reagent.core :as reagent]
    [baking-soda.bootstrap3 :as bstrap])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

;; notifications

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

;; disks tree

(defn- tree-node [root path-coll]
  (let [node (reagent/cursor root path-coll)
        {:keys [children expanded? label path]} @node
        collapse-directory! (fn [_]
                              (swap! node dissoc :expanded?))
        expand-directory! (fn [_]
                            (swap! node assoc :expanded? true)
                            (when (and
                                    (seq path-coll)
                                    (or
                                      (keyword? children)
                                      (empty? children)))
                              (swap! node assoc :children ::loading)
                              (go
                                (try
                                  (let [disk (->> path-coll (take 2) (get-in @root) :disk)
                                        {children :children} (utils/<? (net/get-metadata disk path))]
                                    (swap! node assoc :children children))
                                  (catch :default e
                                    (swap! node assoc :children ::error)
                                    (error e))))))]
    [:li.list-group-item
      (if children
        ;; directory node
        [:span.list-group-item-text
          {:on-click (if expanded? collapse-directory! expand-directory!)}
          [bstrap/Glyphicon {:glyph (if expanded?
                                      (case children
                                        ::loading "refresh"
                                        ::error "exclamation-sign"
                                        "folder-open")
                                      "folder-close")}]
          label]
        ;; leaf node
        [:span.list-group-item-text
          [bstrap/Glyphicon {:glyph "music"}]
          label])
      (when (and
              expanded?
              (coll? children)
              (seq children))
        [:ul.list-group
          (map-indexed
            (fn [i _]
              ^{:key i} [tree-node root (conj path-coll :children i)])
            children)])]))

(defn disks-tree-comp [disks-store]
  (reagent/with-let [tuple->node (fn [[label disk]]
                                   {:children []
                                    :disk disk
                                    :label label
                                    :path "/"})
                     root (reagent/atom {:children (mapv tuple->node @disks-store)
                                         :expanded? true
                                         :label "My disks"})
                     store-watcher (fn [_ _ old-state new-state]
                                     (let [[removed added _] (diff old-state new-state)
                                           added (map tuple->node added)]
                                       (swap! root update-in [:children] into added)))
                     _ (add-watch disks-store ::tree-comp store-watcher)]
    [:div.tree
      [:ul.list-group
        [tree-node root []]]]
    (finally
      (remove-watch ::tree-comp disks-store))))

;; disks management

(defn add-disk-comp []
  [bstrap/Button
    {:bs-size "small"
     :bs-style "default"
     :on-click net/ydisk-oauth-redirect!}
    [bstrap/Glyphicon {:glyph "plus"}]
    "Add YDisk..."])

;; init

(defn init-ui! [disks-store]
  (reagent/render [add-disk-comp] (.getElementById js/document "add-disk"))
  (reagent/render [disks-tree-comp disks-store] (.getElementById js/document "disks-tree")))
