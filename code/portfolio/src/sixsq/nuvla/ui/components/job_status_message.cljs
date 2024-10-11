(ns sixsq.nuvla.ui.components.job-status-message
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.job.views :refer []]))

(defscene simple-tooltip
  [WithTooltip
   [:label "Component with tooltip"]
   "Tooltip content"])

(defscene longtext-tooltip
  [WithTooltip
   [:label "Component with long text tooltip"]
   "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer vitae varius velit."])
