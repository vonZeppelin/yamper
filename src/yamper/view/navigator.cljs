(ns yamper.view.navigator
  (:require
   [ant-man.core :as ant]
   [cljs.core.async :refer [<!] :refer-macros [go]]
   [cljs.core.match :refer-macros [match]]
   [reagent.core :as r]
   [yamper.net :as net]))

(defn navigator-comp [app-state]
  (r/with-let [hidden? (r/cursor app-state [:navigator :hidden])
               tree-data (r/cursor app-state [:navigator :tree-data])
               disks-menu (r/as-element
                           [ant/menu
                            {:onClick #(-> % .-key net/disk-registrators (apply []))}
                            (for [[registrator-name _] net/disk-registrators]
                              [ant/menu-item {:key registrator-name}
                               [ant/icon {:type "hdd"}]
                               registrator-name])])
               dir-or-audio? (fn [item]
                               (match item
                                 {:children (_ :guard some?)} true
                                 {:type "audio"} true
                                 :else false))
               to-item-path (fn [tree-node]
                              (->> (.. tree-node -props -dataref) js->clj (interpose :children)))
               to-disk-item (fn [[disk-index & _]]
                              (@tree-data disk-index))
               load-tree-data (fn [node]
                                (js/Promise. (fn [resolve _]
                                               (let [item-path (to-item-path node)
                                                     {disk :disk} (to-disk-item item-path)
                                                     {item-disk-path :path} (get-in @tree-data item-path)]
                                                 (if (empty? (.. node -props -children))
                                                   (go
                                                     (match (<! (net/browse disk item-disk-path))
                                                       [:ok {:children children}] (swap! tree-data
                                                                                         update-in item-path assoc
                                                                                         :children (filterv
                                                                                                    dir-or-audio?
                                                                                                    children))
                                                       [:error err] (error err))
                                                     (resolve))
                                                   (resolve))))))
               render-tree-nodes (fn render-tree-nodes [items parent-path disk-name]
                                   (map-indexed
                                    (fn [idx {:keys [title path children]}]
                                      (let [disk-name (or disk-name title)
                                            item-path (conj parent-path idx)
                                            node-props {:title (if (empty? parent-path)
                                                                 (r/as-element
                                                                  [:span.disk-dir
                                                                   title
                                                                   [ant/icon
                                                                    {:onClick print
                                                                     :theme "filled"
                                                                     :type "close-circle"}]])
                                                                 title)
                                                        :key (str disk-name path)
                                                        :dataref item-path
                                                        :selectable false}]
                                        (if children
                                          [ant/tree-tree-node
                                           node-props
                                           (render-tree-nodes children item-path disk-name)]
                                          [ant/tree-tree-node
                                           (assoc node-props :isLeaf true)])))
                                    items))
               add-to-playlist (fn [event]
                                 (let [item-path (to-item-path (.-node event))
                                       {disk-name :title disk :disk} (to-disk-item item-path)
                                       {item-children :children :as item} (get-in @tree-data item-path)
                                       to-song (fn [{item-title :title item-disk-path :path}]
                                                 {:title item-title
                                                  :key (str disk-name item-disk-path)
                                                  :path item-disk-path
                                                  :duration "--:--"
                                                  :disk disk})
                                       files (if item-children
                                               (filter :type item-children)
                                               [item])]
                                   (swap! app-state
                                          update-in [:playlist :tracks] into
                                          (map to-song files))))
               _ (add-watch net/disks-store ::navigator print)]
    [ant/layout-sider
     {:collapsedWidth "16"
      :collapsible true
      :onCollapse (fn [collapsed _]
                    (reset! hidden? collapsed))
      :width "30%"}
     [ant/layout {:style {:display (when @hidden? "none")}}
      [ant/dropdown {:overlay disks-menu}
       [ant/button
        "Add new disk"
        [ant/icon {:type "down"}]]]
      [ant/tree-directory-tree
       {:blockNode true
        :expandAction "doubleClick"
        :loadData load-tree-data
        :onRightClick add-to-playlist}
       (render-tree-nodes @tree-data [] nil)]]]
    (finally
      (remove-watch ::navigator net/disks-store))))
