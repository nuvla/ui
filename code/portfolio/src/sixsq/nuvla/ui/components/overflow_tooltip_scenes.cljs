(ns sixsq.nuvla.ui.components.overflow-tooltip-scenes
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.utils.tooltip :refer [WithOverflowTooltip]]))

(defscene overflow-tooltip
  [WithOverflowTooltip
   {:as :div.max-width-12ch.ellipsing
    :content "Component with overflow tooltip"
    :tooltip [:p "Tooltip content"]}])
