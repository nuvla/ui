(ns sixsq.nuvla.ui.portfolio-utils
  (:require [portfolio.reagent :as p]))

(defmulti scene-root identity)

(defmacro defscene [id & body]
  (let [scene-id  (str (-> &env :ns :name) "." (name id))
        scene-url (str "/scene.html?id=" scene-id)]
    `(do (p/defscene ~id
           [:iframe.scene-canvas {:src      ~scene-url
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
         (defmethod scene-root ~scene-id
           []
           [~id]))))
