(ns yamper.view
  (:require
   [ant-man.core :as ant]
   [cljs.core.async :refer [<!] :refer-macros [go]]
   [cljs.core.match :refer-macros [match]]
   [reagent.core :as reagent]
   [yamper.net :as net]))

(defn ^:export success [msg]
  (.success js/antd.notification (js-obj "description" msg "message" "Hooray!")))

(defn ^:export error [msg]
  (.error js/antd.notification (js-obj "description" msg "message" "Something went wrong...")))

(def ^:private yandex-logo [:svg {:viewBox "0 0 320 512" :width "0.625em" :fill "currentColor"}
                            [:path {:d "M129.5 512V345.9L18.5 48h55.8l81.8 229.7L250.2 0h51.3L180.8 347.8V512h-51.3z"}]])

(def ^:private app-state (reagent/atom {:navigator {:hidden false
                                                    :tree-data (mapv
                                                                (fn [[disk-name disk]]
                                                                  {:title disk-name
                                                                   :path "/"
                                                                   :children []
                                                                   :disk disk})
                                                                @net/disks-store)}}))

(defn- header-comp []
  [ant/layout-header
   [:h1.logo
    [ant/icon {:component (reagent/reactify-component (constantly yandex-logo))}]
    "amper"]])

(defn- navigator-comp []
  (reagent/with-let [hidden? (reagent/cursor app-state [:navigator :hidden])
                     tree-data (reagent/cursor app-state [:navigator :tree-data])
                     disks-menu (reagent/as-element
                                 [ant/menu
                                  {:onClick #(-> % .-key net/disk-registrators (apply []))}
                                  (for [[registrator-name _] net/disk-registrators]
                                    [ant/menu-item
                                     {:key registrator-name}
                                     [ant/icon {:type "hdd"}]
                                     registrator-name])])
                     load-tree-data (fn [node]
                                      (js/Promise. (fn [resolve _]
                                                     (let [node-path (->> (.. node -props -dataRef) js->clj (interpose :children))
                                                           disk (->> node-path first (nth @tree-data) :disk)
                                                           disk-path (->> node-path (get-in @tree-data) :path)]
                                                       (if (empty? (.. node -props -children))
                                                         (go
                                                           (match (<! (net/browse disk disk-path))
                                                             [:ok {:children children}] (swap! tree-data
                                                                                               update-in node-path
                                                                                               assoc :children children)
                                                             [:error err] (error err))
                                                           (resolve))
                                                         (resolve))))))
                     render-tree-nodes (fn render-tree-nodes [items parent-path disk-name]
                                         (map-indexed
                                          (fn [index {:keys [title path children]}]
                                            (let [disk-name (or disk-name title)
                                                  node-key (str disk-name path)
                                                  node-path (conj parent-path index)]
                                              (if children
                                                [ant/tree-tree-node
                                                 {:title title :key node-key :dataRef node-path}
                                                 (render-tree-nodes children node-path disk-name)]
                                                [ant/tree-tree-node
                                                 {:title title :key node-key :dataRef node-path :isLeaf true}])))
                                          items))
                     _ (add-watch net/disks-store ::navigator print)]
    [ant/layout-sider
     {:class "navigator"
      :collapsedWidth "16"
      :collapsible true
      :onCollapse (fn [collapsed _]
                    (reset! hidden? collapsed))
      :multiple true
      :width 350}
     (when-not @hidden?
       [ant/layout
        [ant/dropdown
         {:overlay disks-menu}
         [ant/button
          "Add new disk"
          [ant/icon {:type "down"}]]]
        [ant/tree
         {:loadData load-tree-data}
         (render-tree-nodes @tree-data [] nil)]])]
    (finally
      (remove-watch ::navigator net/disks-store))))

(defn- tbd-comp []
  [ant/layout-content
   {:style {:height "100px"}} "Content"])

(defn- app-layout []
  [ant/layout
   [header-comp]
   [ant/layout
    [navigator-comp]
    [tbd-comp]]])

(defn init-ui! []
  (reagent/render
   [app-layout]
   (.getElementById js/document "app")))
