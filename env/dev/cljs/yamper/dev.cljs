(ns ^:figwheel-no-load yamper.dev
  (:require
    [yamper.core :as core]
    [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
