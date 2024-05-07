(ns sixsq.nuvla.ui.pages.apps.views-timeseries
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.apps.apps-application.events :as events]
            [sixsq.nuvla.ui.pages.apps.apps-application.subs :as subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.plot :as plot]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
            [sixsq.nuvla.ui.utils.timeseries-components :as ts-components]))

(def test-query {:query-name "test-query1"
                 :query-type "standard"
                 :query {:aggregations [{:aggregation-name :test-metric1-avg
                                         :aggregation-type "avg"
                                         :field-name "test-metric1"}]}})

(defn timestamp+percentage [ts-data aggregation-name]
  (mapv (fn [d]
          (let [value     (get-in d [:aggregations aggregation-name :value])]
            (assoc d :x (:timestamp d)
                     :y value)))
        ts-data))


(defn LinePlot [{:keys [timespan-option] :as timespan} title label dataset]
  (let [[from to] (if-not (= "custom period" timespan-option)
                    (ts-utils/timespan-to-period (:timespan-option timespan))
                    [(:from timespan) (:to timespan)])]
    [plot/Line {:updateMode "none"
                :data       {:datasets [{:data            dataset
                                         :label           label
                                         :backgroundColor (first plot/default-colors-palette)
                                         :borderColor     (first plot/default-colors-palette)
                                         :borderWidth     1}]}

                :options    {:plugins  {:title {:display  true
                                                :text     title
                                                :position "top"}}
                             :elements {:point {:radius 1}}

                             :scales   {:x     {:type "time"
                                                :min  from
                                                :max  to
                                                :time {:unit (case (:timespan-option timespan)
                                                               ("last 15 minutes"
                                                                 "last hour"
                                                                 "last 6 hours") "minute"
                                                               "last day" "hour"
                                                               "last year" "month"
                                                               "day")}
                                                :title {:display "true"
                                                        :text    "Time"}}
                                        :y     {:min 0}}}}]))

(defn DataPane []
  (let [loading?              (subscribe [::subs/loading?])
        app-data              (subscribe [::subs/app-data])
        fetch-app-data        (fn [timespan]
                                (let [[from to] (ts-utils/timespan-to-period timespan)]
                                  (dispatch [::events/set-selected-timespan {:timespan-option timespan
                                                                             :from            from
                                                                             :to              to}])))

        tr                    (subscribe [::i18n-subs/tr])
        export-modal-visible? (r/atom false)
        aggregations          (-> test-query :query :aggregations)
        selected-timespan     (subscribe [::subs/timespan])]
    (fetch-app-data (first ts-utils/timespan-options))
    (fn []
      (let [ ts-data               (:ts-data (first (get @app-data :test-query1)))
            {:keys [aggregation-name field-name]} (first aggregations)]
        [:div
         [ui/Menu {:width      "100%"
                   :borderless true}
          [ui/MenuMenu {:position "left"}
           [ts-components/TimeSeriesDropdown {:loading?         @loading?
                                              :default-value    (first ts-utils/timespan-options)
                                              :timespan-options ts-utils/timespan-options
                                              :on-change-event  ::events/set-selected-timespan}]]
          [ui/MenuItem {:icon     icons/i-export
                        :position "right"
                        :content  (str (@tr [:export-data]) " (.csv)")
                        :on-click #(reset! export-modal-visible? true)}]]
         [ui/TabPane

          [ui/Grid {:centered true
                    :padded true}
           [ui/GridColumn
            [LinePlot @selected-timespan
             (str "Average " field-name)
             field-name
             (timestamp+percentage ts-data aggregation-name)]]]]]))))