(ns sixsq.nuvla.ui.components.pagination-scenes
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.plugins.pagination-refactor :as pagination]
            [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn ItemsCountSelector
  [!items-count]
  [:div
   "Number of items to paginate: "
   [ui/Input {:class     :total-items-input
              :type      :number
              :value     @!items-count
              :on-change (ui-callback/input-callback #(reset! !items-count %))}]])

(defscene basic-pagination
  (r/with-let [!items-count (r/atom 100)
               !pagination  (r/atom {:page-index 0
                                     :page-size  10})]
    [:div {:style {:margin 10}}
     [ItemsCountSelector !items-count]
     [pagination/PaginationController
      {:total-items @!items-count
       :!pagination !pagination}]]))
