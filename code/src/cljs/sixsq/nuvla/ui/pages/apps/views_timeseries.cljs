(ns sixsq.nuvla.ui.pages.apps.views-timeseries
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.apps.apps-application.events :as events]
            [sixsq.nuvla.ui.pages.apps.apps-application.subs :as subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
            [sixsq.nuvla.ui.utils.timeseries-components :as ts-components]))

(defn DataPane []
  (let [ts-id "timeseries/f9f76bdd-56e9-4dde-bbcf-30d1b84625e0"
        query-name "test-query1"
        from        "2024-04-21T00:00:00.000Z"
        to "2024-04-24T23:59:59.000Z"
        granularity "1-days"
        loading?      (subscribe [::subs/loading?])
        app-data (subscribe [::subs/app-data])
        fetch-app-data          (fn [timespan]
                                    (let [[from to] (ts-utils/timespan-to-period timespan)]
                                      (js/console.log from to)
                                      (dispatch [::events/set-selected-timespan {:timespan-option timespan
                                                                                 :from from
                                                                                 :to to}])))

        tr       (subscribe [::i18n-subs/tr])
        export-modal-visible? (r/atom false)]
    (fetch-app-data (first ts-utils/timespan-options))
    (fn []
      [:div
       [ui/Menu {:width      "100%"
                 :borderless true}
        [ui/MenuMenu {:position "left"}
         [ts-components/TimeSeriesDropdown {:loading?         @loading?
                                            :default-value    (first ts-utils/timespan-options)
                                            :timespan-options ts-utils/timespan-options
                                            :on-change-event ::events/set-selected-timespan}]]
        [ui/MenuItem {:icon     icons/i-export
                      :position "right"
                      :content  (str (@tr [:export-data]) " (.csv)")
                      :on-click #(reset! export-modal-visible? true)}]]

       [ui/Grid (into [:div] (mapv :timestamp (:ts-data (first (:test-query1 @app-data)))))]]
      )))