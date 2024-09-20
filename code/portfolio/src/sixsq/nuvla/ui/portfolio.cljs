(ns sixsq.nuvla.ui.portfolio
  (:require [portfolio.ui :as portfolio]
            [sixsq.nuvla.ui.components.button-scenes]
            [sixsq.nuvla.ui.components.table-scenes]
            [sixsq.nuvla.ui.components.tooltip-scenes]
            [sixsq.nuvla.ui.components.overflow-tooltip-scenes]))

(portfolio/start!
  {:config
   {:css-paths ["/ui/css/semantic.min.css"
                "/ui/css/nuvla-ui.css"
                "/ui/css/react-datepicker.min.css"]
    :background/default-option-id :light-mode}})

(defn init [])
