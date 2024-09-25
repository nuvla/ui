(ns sixsq.nuvla.ui.portfolio
  (:require [portfolio.ui :as portfolio]
            [re-frame.core :refer [dispatch-sync]]
            [sixsq.nuvla.ui.db.events :as db-events]
            [sixsq.nuvla.ui.common-components.i18n.events :as i18n-events]
            [sixsq.nuvla.ui.components.button-scenes]
            [sixsq.nuvla.ui.components.table-scenes]
            [sixsq.nuvla.ui.components.tooltip-scenes]
            [sixsq.nuvla.ui.components.overflow-tooltip-scenes]
            [sixsq.nuvla.ui.components.table-refactor-scenes]
            [sixsq.nuvla.ui.components.tanstack-table-scenes]))

(portfolio/start!
  {:config
   {:canvas-path "/ui/portfolio.html"
    ;:css-paths ["/ui/css/semantic.min.css"
    ;            "/ui/css/nuvla-ui.css"
    ;            "/ui/css/react-datepicker.min.css"]
    :background/default-option-id :light-mode}})

(defn init []
  (dispatch-sync [::db-events/initialize-db])
  (dispatch-sync [::i18n-events/set-locale]))
