(ns sixsq.nuvla.ui.pages.apps.views-timeseries
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.apps.apps-application.events :as events]
            [sixsq.nuvla.ui.pages.apps.apps-application.subs :as subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.plot :as plot]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
            [sixsq.nuvla.ui.utils.timeseries-components :as ts-components]))

(def test-query {:query-name "test-query1"
                 :query-type "standard"
                 :query {:aggregations [{:aggregation-name "test-metric1-avg"
                                         :aggregation-type "avg"
                                         :field-name "test-metric1"}]}})

(def aggregation-types {"avg" "Average"})

(defn ExportDataModal [{:keys [on-close]}]
  (r/with-let [state (r/atom {:form-data               {}
                              :custom-period-selected? false})]
    (fn []
      (let [tr                    (subscribe [::i18n-subs/tr])
            aggregations          (-> test-query :query :aggregations)]
        [ui/Modal {:close-icon true
                   :open       true
                   :onClose    on-close}
         [ui/ModalHeader (@tr [:export-data])]
         [ui/ModalContent
          [ui/ModalDescription
           [:div
            [ui/Header {:as       "h4"
                        :attached "top"
                        :style    {:background-color "#00000008"}}
             "Metrics to export"]
            [ui/Segment {:attached true}
             [ui/Table {:basic       "very"}
              [ui/TableHeader
               [ui/TableRow
                [ui/TableHeaderCell "Name"]
                [ui/TableHeaderCell {:textAlign "right"}
                 "Aggregation type"]]]
              [ui/TableBody
               (for [{:keys [field-name aggregation-type]} aggregations]
                 [ui/TableRow
                  [ui/TableCell field-name]
                  [ui/TableCell {:textAlign "right"} (get aggregation-types aggregation-type)]])]]]]
           [ui/Form
            [:div
             [ui/Header {:as       "h4"
                         :attached "top"
                         :style    {:background-color "#00000008"}}
              (str/capitalize (@tr [:period]))
              [ui/HeaderSubheader "Choose a period that you would like to export data (in .csv format) for."]]
             (into [ui/Segment {:attached true}]
                   (conj (mapv (fn [option]
                                 (when-not (= "custom period" option)
                                   [ui/FormField
                                    [ui/Radio
                                     {:label     (@tr [(ts-utils/format-option option)])
                                      :name      "radioGroupTimespan"
                                      :value     option
                                      :checked   (= (get-in @state [:form-data :timespan-option])
                                                    option)
                                      :on-change (fn [_e t]
                                                   (let [[from to] (ts-utils/timespan-to-period (. t -value))]
                                                     (swap! state assoc :custom-period-selected? false)
                                                     (swap! state assoc-in [:form-data :timespan-option] (. t -value))
                                                     (swap! state assoc-in [:form-data :from] from)
                                                     (swap! state assoc-in [:form-data :to] to)))}]]))
                               ts-utils/timespan-options)
                         [:div {:style {:display     "flex"
                                        :align-items "center"}}
                          [ui/FormField
                           [ui/Radio
                            {:label          (@tr [(ts-utils/format-option "custom period")])
                             :name           "radioGroupTimespan"
                             :value          "custom period"
                             :checked        (= (get-in @state [:form-data :timespan-option])
                                                "custom period")
                             :on-change (fn [_e t]
                                          (swap! state assoc-in [:form-data :timespan-option] (. t -value))
                                          (swap! state update-in [:form-data] dissoc :from :to)
                                          (swap! state assoc :custom-period-selected? true))}]]
                          [:div {:style {:display       "flex"
                                         :margin-bottom 10
                                         :visibility    (if (:custom-period-selected? @state)
                                                          "visible"
                                                          "hidden")}}
                           [sixsq.nuvla.ui.utils.timeseries-components/CustomPeriodSelector (:form-data @state)
                            {:on-change-fn-from #(swap! state assoc-in [:form-data :from] %)
                             :on-change-fn-to   #(swap! state assoc-in [:form-data :to] %)}]]]))]]]]
         [ui/ModalActions
          [uix/Button {:text     (@tr [:export])
                       :icon     icons/i-export
                       :positive true
                       :disabled (or (not (-> @state :form-data :from))
                                     (not (-> @state :form-data :to)))
                       :active   true
                       :on-click #(let [{:keys [from to] :as form-data} (:form-data @state)]
                                    (dispatch [::events/fetch-app-data-csv
                                               {:from        from
                                                :to          to
                                                :granularity (ts-utils/granularity-for-timespan form-data)
                                                :query       (:query-name test-query)}]))}]]]))))

(defn timestamp+value [ts-data aggregation-name]
  (mapv (fn [d]
          (let [value     (get-in d [:aggregations (keyword aggregation-name) :value])]
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
                             :elements {:point {:radius 2}}

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
        selected-timespan     (subscribe [::subs/timespan])
        query-name            (keyword (:query-name test-query))]
    (fetch-app-data (first ts-utils/timespan-options))
    (fn []
      (let [ts-data               (-> @app-data
                                      query-name
                                      (first)
                                      :ts-data)]
        [:div {:class :uix-apps-details-details}
         [:div {:style {:display "flex"
                        :align-items "center"}} [:h4 {:class :tab-app-detail} "Data"]
          #_(into [:div {:style {:margin-bottom 14
                               :margin-left 20}}]
                (mapv (fn [m] [ui/Label  m]) metrics))]
         [ui/Menu {:width      "100%"
                   :borderless true
                   :style {:background-color "#F9FAFB"}}
          [ui/MenuMenu {:position "left"}
           [ts-components/TimeSeriesDropdown {:loading?         @loading?
                                              :default-value    (first ts-utils/timespan-options)
                                              :timespan-options ts-utils/timespan-options
                                              :on-change-event  ::events/set-selected-timespan}]]
          [ui/MenuItem {:icon     icons/i-export
                        :position "right"
                        :content  (str (@tr [:export-data]) " (.csv)")
                        :on-click #(reset! export-modal-visible? true)}]]
         (when @export-modal-visible?
           [ExportDataModal {:on-close #(reset! export-modal-visible? false)}])

         (into [ui/Grid {:centered true
                         :columns  2
                         :padded   true}]
               (mapv (fn [{:keys [aggregation-name field-name aggregation-type]}]
                       [ui/GridColumn
                        [ui/Segment {:style  {:background-color "#F9FAFB"}}
                         [LinePlot @selected-timespan
                          (str (get aggregation-types aggregation-type) " " field-name)
                          field-name
                          (timestamp+value ts-data aggregation-name)]]])
                     aggregations))]))))