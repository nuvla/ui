(ns sixsq.nuvla.ui.stories.components.overflow-tooltip-stories
  (:require [sixsq.nuvla.ui.stories.helper :as helper]
            [sixsq.nuvla.ui.utils.tooltip :refer [with-overflow-tooltip]]
            [reagent.core :as reagent]))

(defn OverflowTooltipWrapper
  [tooltip-text overflow-tooltip]
  [with-overflow-tooltip
   [:div (cond-> {:data-testid "test-element"}
                 overflow-tooltip
                 (assoc :style {:max-width "110px"
                                :overflow  "hidden"
                                :text-wrap "nowrap"}))
    (if overflow-tooltip
      "Label with overflowing text"
      "Short label")]
   [:div tooltip-text]])

(defn ^:export overflowTooltip [args]
  (let [params           (-> args helper/->params)
        tooltip-text     (:tooltipText params)
        overflow-tooltip (:overflowTooltip params)]
    (reagent/as-element
      [OverflowTooltipWrapper tooltip-text overflow-tooltip])))
