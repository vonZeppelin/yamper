(ns yamper.utils)

(defmacro <? [ch]
  `(yamper.utils/throw-error (cljs.core.async/<! ~ch)))
