(ns yamper.core
  (:require
    [alandipert.storage-atom :as storage]
    [yamper.net :as net]
    [yamper.utils :as utils]
    [yamper.view :as view])
  (:require-macros
    [cljs.core.async.macros :as async]))

(def disks-store (storage/local-storage (atom #{}) :disks))

(defn init! []
  (try
    (when-let [disk (net/register-ydisk)]
      (async/go
        (try
          (swap! disks-store conj disk)
          (view/success
            (utils/format
              "New YDisk registered: <b>%s</b>"
              (utils/<? (net/disk-name disk))))
         (catch :default e
           (view/error e)))))
    (catch :default e
      (view/error e)))
  (view/init-ui! disks-store))
