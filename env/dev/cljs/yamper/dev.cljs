(ns ^:figwheel-no-load yamper.dev
  (:require
   [devtools.core :as devtools]
   [yamper.core :as core]))

(enable-console-print!)

(devtools/install!)

(core/init!)
