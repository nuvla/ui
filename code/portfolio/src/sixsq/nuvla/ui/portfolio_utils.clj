(ns sixsq.nuvla.ui.portfolio-utils
  (:require [portfolio.reagent :as p]))

(defmulti scene-root identity)

(defmacro defscene [id & body]
  `(do (p/defscene ~id
         [:iframe.scene-canvas {:src      (str "/scene.html?id=" ~(name id))
                                :style    {:border           :none
                                           :flex-grow        1
                                           :width            "100%"
                                           :min-height       "500px"
                                           :background-color "rgb(255, 255, 255)"
                                           :background-image :none}
                                :data-src '~@body}])
       (defn ~id
         []
         ~@body)
       (defmethod scene-root ~(name id)
         []
         [~id])))
