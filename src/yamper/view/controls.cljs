(ns yamper.view.controls
  (:require
   [ant-man.core :as ant]
   cljsjs.howler
   [reagent.core :as r]
   [yamper.view.common :refer [iconfont]]))

(defn- set-volume [value]
  (.volume js/Howler (/ value 100)))

(defn- get-volume []
  (* 100 (.volume js/Howler)))

(defn controls-comp []
  [ant/layout-header
   [ant/row
    [ant/col {:span 2}
     [:h1.logo
      [:> iconfont {:type "logo"}]
      "amper"]]
    [ant/col {:span 14}
     [ant/slider]]
    [ant/col {:span 1}
     [ant/switch
       {:checkedChildren (r/create-element iconfont #js{:type "volume-on"})
        :defaultChecked true
        :onChange (fn [checked _]
                    (.mute js/Howler (not checked)))
        :unCheckedChildren (r/create-element iconfont #js{:type "volume-off"})}]]
    [ant/col {:span 5}
     [ant/slider {:defaultValue (get-volume)
                  :onChange set-volume}]]
    [ant/col {:className "playback-controls" :span 2}
     [ant/button {:icon "step-backward" :shape "circle"}]
     [ant/button {:icon "caret-right" :shape "circle" :size "large"}]
     [ant/button {:icon "step-forward" :shape "circle"}]]]])
