(ns yamper.core
  (:require
    [alandipert.storage-atom :as storage]
    [yamper.net :as net]
    [yamper.utils :as utils]
    [yamper.view :as view])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(def disks-store (storage/local-storage
                   (atom (sorted-set-by first))
                   :disks))

(defn init! []
  (try
    (when-let [ydisk (net/try-register-ydisk)]
      (go
        (try
          (let [disk-name (utils/<? (net/disk-name ydisk))]
            (swap! disks-store conj [disk-name ydisk])
            (view/success (utils/format
                            "New YDisk registered: <b>%s</b>"
                            (utils/html-escape disk-name))))
         (catch :default e
           (view/error e)))))
    (catch :default e
      (view/error e)))
  (view/init-ui! disks-store))
