(ns sixsq.nuvla.ui.utils.tooltip
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn with-tooltip
  [component tooltip]
  (if tooltip
    [ui/Popup
     {:content   (r/as-element [:p tooltip])
      :trigger   (r/as-element component)
      :hoverable true}]
    component))

(defn with-overflow-tooltip
  [_ _]
  (let [ref    (r/atom nil)
        ref-fn #(some->> % (reset! ref))]
    (fn [component tooltip]
      (let [overflowed? (when-let [el @ref]
                          (or (< (.-offsetWidth el) (.-scrollWidth el))
                              (< (.-offsetHeight el) (.-scrollHeight el))))]
        (if overflowed?
          (with-tooltip component tooltip)
          [:div {:ref ref-fn} component])))))
