(ns sixsq.nuvla.ui.utils.tooltip
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn with-tooltip
  [component tooltip]
  (if tooltip
    [ui/Popup
     {:content (r/as-element [:p tooltip])
      :trigger (r/as-element component)}]
    component))
