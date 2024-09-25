(ns sixsq.nuvla.ui.portfolio
  (:require [portfolio.ui :as portfolio]
            [reagent.dom :as rdom]
            [goog.dom :as gdom]
            [sixsq.nuvla.ui.components.tooltip-scenes]
            [sixsq.nuvla.ui.components.overflow-tooltip-scenes]
            [sixsq.nuvla.ui.portfolio-utils :refer [scene-root]]))

(defn init [])

(defn ^:export init-portfolio []
  (portfolio/start!
    {:config
     {:background/default-option-id :light-mode}}))

(defn mount-root [scene-id]
  (rdom/render
    (scene-root scene-id)
    (gdom/getElement "root")))

(defn ^:export init-scene []
  (let [scene-id (-> (js/URLSearchParams. (-> js/document .-location .-search))
                     (.get "id"))]
    (mount-root scene-id)))
