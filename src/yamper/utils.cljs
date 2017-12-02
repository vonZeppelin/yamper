(ns yamper.utils
  (:require
    [goog.string :as gstring]
    [goog.string.format])
  (:require-macros
    yamper.utils))

(defn error? [e]
  (instance? js/Error e))

(defn throw-error [e]
  (if (error? e)
   (throw e)
   e))

(defn format [fmt & args]
  (apply gstring/format fmt args))

(defn html-escape [s]
  (gstring/htmlEscape s))