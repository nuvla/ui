(ns sixsq.nuvla.ui.components.tooltip-scenes
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.utils.tooltip :refer [with-tooltip]]))

(defscene simple-tooltip
  [with-tooltip
   [:label "Component with tooltip"]
   "Tooltip content"])

(defscene longtext-tooltip
  [with-tooltip
   [:label "Component with long text tooltip"]
   "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer vitae varius velit."])
