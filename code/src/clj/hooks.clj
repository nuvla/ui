;; Easiest to just make this a clj file.
(ns hooks
  (:require [lambdaisland.ornament :as o]
            [garden.compiler :as gc]
            [girouette.tw.preflight :as girouette-preflight]))

;; Optional, but it's common to still have some style rules that are not
;; component-specific, so you can use Garden directly for that
(def global-styles
  [[:html {:font-size "14pt"}]])

(defn write-styles-hook
  {:shadow.build/stage :flush}
  [build-state & args]
  ;; In case your global-styles is in a separate clj file you will have to
  ;; reload it yourself, shadow only reloads/recompiles cljs/cljc files
  #_(require my.styles :reload)
  ;; Just writing out the CSS is enough, shadow will pick it up (make sure you
  ;; have a <link href=styles.css rel=stylesheet>)
  (spit "resources/public/ui/css/styles.css"
        (str
         ;; `defined-styles` takes a :preflight? flag, but we like to have some
         ;; style rules between the preflight and the components. This whole bit
         ;; is optional.
         (gc/compile-css (concat
                          girouette-preflight/preflight
                          global-styles))
         "\n"
         (o/defined-styles)))
  build-state)