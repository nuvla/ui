(ns sixsq.nuvla.ui.components.overflow-tooltip-scenes
  (:require [portfolio.reagent :refer-macros [defscene]]
            [sixsq.nuvla.ui.utils.tooltip :refer [with-overflow-tooltip]]))

(defscene overflow-tooltip
  [with-overflow-tooltip
   [:div {:style {:max-width "120px"
                  :overflow :hidden
                  :text-wrap :nowrap}}
    "Component with overflow tooltip"]
   [:p "Tooltip content"]])