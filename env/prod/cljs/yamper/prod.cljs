(ns yamper.prod
  (:require
    [yamper.core :as core]))

(set! *print-fn* (fn [& _]))

(core/init!)
