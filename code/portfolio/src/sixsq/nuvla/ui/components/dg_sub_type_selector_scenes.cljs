(ns sixsq.nuvla.ui.components.dg-sub-type-selector-scenes
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.portfolio-utils :refer [defscene BoolParam]]
            [sixsq.nuvla.ui.common-components.plugins.dg-sub-type-selector :refer [DGSubTypeSelectorController]]))

(defscene basic-selector
  (r/with-let [!value     (r/atom :docker-compose)
               !disabled? (r/atom false)]
    [:div {:style {:margin 10}}
     [BoolParam !disabled? "checkbox-disabled" "Disabled ?"]
     [DGSubTypeSelectorController {:!value     !value
                                   :!disabled? !disabled?}]
     [:p {:data-testid "selected-dg-sub-type"
          :style       {:margin-top 30}}
      "Deployment Group sub type is " [:b (name @!value)]]]))

(defscene sizes
  (r/with-let [!value (r/atom :docker-compose)]
    (into [:div {:style {:margin 100}}]
          (map (fn [size]
                 [:div
                  [:p (str size)]
                  [DGSubTypeSelectorController {:!value !value
                                                :!size  (r/atom size)}]
                  [:hr {:style {:border     :none
                                :border-top "1px dotted"}}]])
               [:small :large :big :huge]))))

