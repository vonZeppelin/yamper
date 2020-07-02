(ns yamper.view.common)

(defn ^:export error [msg]
  (.error js/antd.notification #js{:description msg :message "Something went wrong..."}))

(defn ^:export success [msg]
  (.success js/antd.notification #js{:description msg :message "Hooray!"}))

(def iconfont
  (.createFromIconfontCN js/antd.Icon #js{:scriptUrl "//at.alicdn.com/t/font_1092324_uq57xpwca7.js"}))
