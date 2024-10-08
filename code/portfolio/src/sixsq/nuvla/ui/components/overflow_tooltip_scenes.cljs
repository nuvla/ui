(ns sixsq.nuvla.ui.components.overflow-tooltip-scenes
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.utils.tooltip :refer [WithOverflowTooltip]]))

(defscene overflow-tooltip
  [WithOverflowTooltip
   [:div {:style {:max-width "120px"
                  :overflow :hidden
                  :text-wrap :nowrap}}
    "Component with overflow tooltip"]
   [:p "Tooltip content"]])
