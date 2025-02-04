(ns sixsq.nuvla.ui.portfolio
  (:require [portfolio.ui :as portfolio]
            [re-frame.core :refer [dispatch-sync]]
            [sixsq.nuvla.ui.db.events :as db-events]
            [sixsq.nuvla.ui.common-components.i18n.events :as i18n-events]
            [reagent.dom :as rdom]
            [goog.dom :as gdom]
            [sixsq.nuvla.ui.components.dg-sub-type-selector-scenes]
            [sixsq.nuvla.ui.components.edge-selector-scenes]
            [sixsq.nuvla.ui.components.module-selector-scenes]
            [sixsq.nuvla.ui.components.nav-tab-scenes]
            [sixsq.nuvla.ui.components.resource-filter-scenes]
            [sixsq.nuvla.ui.components.tooltip-scenes]
            [sixsq.nuvla.ui.components.overflow-tooltip-scenes]
            [sixsq.nuvla.ui.components.table-refactor-scenes]
            [sixsq.nuvla.ui.components.job-cell-scenes]
            [sixsq.nuvla.ui.components.msg-scenes]
            [sixsq.nuvla.ui.components.duration-picker-scenes]
            [sixsq.nuvla.ui.components.bulk-progress-monitored-job-scenes]
            [sixsq.nuvla.ui.components.pagination-scenes]
            [sixsq.nuvla.ui.portfolio-utils :refer [Scene]]))

(defn init [])

(defn ^:export init-portfolio []
  (portfolio/start!
    {:config
     {:background/default-option-id :light-mode
      :viewport/defaults
      {:viewport/padding [0 0 0 0]
       :viewport/width   "100%"
       :viewport/height  "100%"}}}))

(defn mount-root [scene-id]
  (rdom/render
    [Scene scene-id]
    (gdom/getElement "root")))

(defn ^:export init-scene []
  (let [scene-id (-> (js/URLSearchParams. (-> js/document .-location .-search))
                     (.get "id"))]
    (dispatch-sync [::db-events/initialize-db])
    (dispatch-sync [::i18n-events/set-locale])
    (mount-root scene-id)))
