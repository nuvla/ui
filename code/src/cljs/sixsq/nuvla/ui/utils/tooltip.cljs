(ns sixsq.nuvla.ui.utils.tooltip
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn WithTooltip
  [Component tooltip]
  [ui/Popup
   {:disabled  (nil? tooltip)
    :content   (r/as-element [:p tooltip])
    :trigger   (r/as-element Component)
    :hoverable true}])

(defn WithOverflowTooltip
  []
  (r/with-let [ref       (atom nil)
               overflow? (r/atom false)
               ref-fn    #(reset! ref %)]
    (r/create-class
      {:display-name        "with-overflow-tooltip"
       :reagent-render      (fn [{:keys [as content tooltip]
                                  :or   {as :div.max-width-26ch.ellipsing}}]
                              [WithTooltip [as {:ref ref-fn} content]
                               (when @overflow? tooltip)])
       :component-did-mount #(reset! overflow? (general-utils/overflowed? @ref))})))
