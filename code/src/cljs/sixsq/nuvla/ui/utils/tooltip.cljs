(ns sixsq.nuvla.ui.utils.tooltip
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn with-tooltip
  [component tooltip]
  (if tooltip
    [ui/Popup
     {:content   (r/as-element [:p {:data-testid "tooltip-content"} tooltip])
      :trigger   (r/as-element component)
      :hoverable true}]
    component))

(defn with-overflow-tooltip
  [_ _]
  (let [ref    (r/atom nil)
        ref-fn #(some->> % (reset! ref))]
    (fn [component tooltip]
      (let [overflowed? (when-let [el @ref]
                          (prn :el el)
                          (prn :a (.-clientWidth el) (.-scrollWidth el))
                          (prn :b (.-clientHeight el) (.-scrollHeight el))
                          (or (< (.-clientWidth el) (.-scrollWidth el))
                              (< (.-clientHeight el) (.-scrollHeight el))))]
        (if overflowed?
          (with-tooltip component tooltip)
          [:div {:ref ref-fn} component])))))
