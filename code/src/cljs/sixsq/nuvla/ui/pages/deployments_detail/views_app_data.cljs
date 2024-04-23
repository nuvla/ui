(ns sixsq.nuvla.ui.pages.deployments-detail.views-app-data
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.pages.deployments-detail.subs :as subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
            [sixsq.nuvla.ui.utils.timeseries-components :as ts-components]))


(defn AppData []
  (let [loading?                  (subscribe [::subs/loading?])]
    [:div
     [ui/Menu {:width "100%"
               :borderless true}
      [ui/MenuMenu {:position "left"}
       [ts-components/TimeSeriesDropdown {:loading?         @loading?
                                          :default-value    (first ts-utils/timespan-options)
                                          :timespan-options ts-utils/timespan-options}]]]

     [ui/TabPane
      "hello"]]))
