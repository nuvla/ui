(ns sixsq.nuvla.ui.stories.components.tooltip-stories
  (:require [cljs.pprint :as pp]
            [sixsq.nuvla.ui.stories.helper :as helper]
            [sixsq.nuvla.ui.utils.tooltip :refer [with-tooltip with-overflow-tooltip]]
            [reagent.core :as reagent]))

(defn TooltipWrapper
  [tooltip-text]
  [with-tooltip
   [:div {:data-testid "test-element"} "Label with tooltip"]
   [:div tooltip-text]])

(defn ^:export tooltip [args]
  (let [params           (-> args helper/->params)
        tooltip-text     (:tooltipText params)]
    (reagent/as-element
      [TooltipWrapper tooltip-text])))

(def ^:export sourceCode
  (with-out-str
    (pp/pprint ['with-tooltip
                [:div "Component with tooltip"]
                [:div "Tooltip text"]])))
