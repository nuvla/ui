(ns sixsq.nuvla.ui.pages.edges.views-timeseries
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.pages.edges-detail.views-timeseries :as edges-detail-timeseries]
    [sixsq.nuvla.ui.pages.edges.events :as events]
    [sixsq.nuvla.ui.pages.edges.subs :as subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.icons :as icons]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
    [sixsq.nuvla.ui.utils.timeseries-components :as ts-components]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))

(defn timestamp+value [data value-key]
  (mapv (fn [d]
          (assoc d :x (:timestamp d)
                   :y (get-in d [:aggregations value-key :value]))) data))

(defn info-edge [bucket]
  (when bucket
    {:name       (:name bucket)
     :id         (:key bucket)
     :avg-online (get-in bucket [:edge-avg-online :value])}))

(defn OnlineStatsByEdge [{:keys [on-close]}]
  (let [fleet-stats          (subscribe [::subs/fleet-stats])
        tr                   (subscribe [::i18n-subs/tr])
        stats-by-edge        (:availability-by-edge @fleet-stats)
        ts-data              (ts-utils/data->timeseries-data stats-by-edge)
        n                    10
        least-available-edge (->> (first ts-data)
                                  :aggregations
                                  :by-edge
                                  :buckets
                                  (filter (fn [{:keys [edge-avg-online]}]
                                            (< (:value edge-avg-online) 1)))
                                  (sort-by (comp :value :edge-avg-online))
                                  (take n))]
    [ui/Card {:style {:overflow "hidden"}}
     [ui/CardContent {:style {:background-color "#F9FAFB"}}
      [ui/CardHeader {:style {:display         "flex"
                              :align-items     "start"
                              :justify-content "space-between"}}
       [:span (@tr [:least-available-nuvlaedges])]
       [icons/CloseIcon {:link     true
                         :color    "black"
                         :on-click on-close}]]
      [ui/CardMeta {:style {:font-size "tiny"}}
       (str "on " (time/time->format (:timestamp (first ts-data))))]]
     [ui/CardContent
      [ui/CardDescription {:style {:overflow-x "auto"}}
       (if (seq least-available-edge)
         [ui/Table {:basic       "very"
                    :unstackable true}
          [ui/TableHeader
           [ui/TableRow
            [ui/TableHeaderCell (str/capitalize (@tr [:name]))]
            [ui/TableHeaderCell {:textAlign "right"
                                 :style     {:white-space "nowrap"}}
             (str (@tr [:availability]) " (%)")]]]
          [ui/TableBody
           (for [bucket least-available-edge]
             (when-let [{:keys [name id avg-online]} (info-edge bucket)]
               ^{:key (str "least-available-" id)}
               [ui/TableRow
                [ui/TableCell
                 [values/AsLink (str (general-utils/id->uuid id)
                                     "?edges-detail-tab=historical-data") :page "edges" :label name]]
                [ui/TableCell {:textAlign "right"} (int (* 100 avg-online))]]))]]
         [:span (@tr [:no-data-to-show])])]]]))

(defn timespan->unit
  [timespan]
  (condp = (:timespan-option timespan)
    ts-utils/timespan-last-15m "minute"
    ts-utils/timespan-last-hour "minute"
    ts-utils/timespan-last-6h "minute"
    ts-utils/timespan-last-day "hour"
    ts-utils/timespan-last-year "month"
    "day"))

(defn FleetStatusTimeSeries [{:keys [timespan-option] :as timespan} data]
  (r/with-let [extra-info-visible? (r/atom false)]
    (let [tr      (subscribe [::i18n-subs/tr])
          ts-data (ts-utils/data->timeseries-data data)
          [from to] (if-not (= ts-utils/timespan-custom timespan-option)
                      (ts-utils/timespan-to-period (:timespan-option timespan))
                      [(:from timespan) (:to timespan)])]
      [ui/Grid {:padded    true
                :columns   2
                :stackable true}
       [ui/GridRow {:centered true}
        [ui/GridColumn {:width     10
                        :textAlign "center"}
         [ui/Segment {:style  {:background-color "#F9FAFB"}}
          [plot/Bar {:data    {:datasets [{:data            (timestamp+value ts-data :virtual-edges-online)
                                           :label           (@tr [:available])
                                           :backgroundColor "#21d32c88"}
                                          {:data            (timestamp+value ts-data :virtual-edges-offline)
                                           :label           (@tr [:unavailable])
                                           :backgroundColor "#eab81198"}]}

                     :options {:plugins  {:title    {:text    (@tr [:fleet-availability])
                                                     :display true}
                                          :subtitle {:text    (@tr [:availability-commissioned-nuvlaedges])
                                                     :display true}}
                               :scales   {:x {:type    "time"
                                              :min     from
                                              :max     to
                                              :grid    {:display false}
                                              :time    {:unit (timespan->unit timespan)}
                                              :title   {:display "true"
                                                        :text    (@tr [:time])}
                                              :stacked true}
                                          :y {:max     (get-in data [:dimensions :nuvlaedge-count])
                                              :min     0
                                              :title   {:display "true"
                                                        :text    (@tr [:number-of-nuvlaedges])}
                                              :stacked true}}
                               :elements {:point {:radius 1}}
                               :onClick  (fn [_evt element _chart]
                                           (when-let [raw-data (js->clj (.. (first element) -element -$context -raw)
                                                                        :keywordize-keys true)]
                                             (let [from        (time/parse-iso8601 (:timestamp raw-data))
                                                   granularity (ts-utils/granularity-for-timespan timespan)
                                                   to          (ts-utils/add-time from granularity)]
                                               (dispatch [::events/fetch-fleet-stats-by-edge
                                                          {:from        from
                                                           :to          to
                                                           :granularity granularity}])
                                               (reset! extra-info-visible? true))))
                               :onHover  (fn [evt chartElement]
                                           (let [cursor (if (first chartElement)
                                                          "pointer"
                                                          "default")]
                                             (set! (.. evt -native -target -style -cursor) cursor)))}}]]
         [edges-detail-timeseries/GraphLabel timespan]]

        [ui/GridColumn {:width 4}
         [:div {:style {:visibility (if @extra-info-visible? "visible" "hidden")
                        :min-width  250}}
          [OnlineStatsByEdge {:on-close #(reset! extra-info-visible? false)}]]]]])))

(defn FleetTimeSeries []
  (let [loading?                  (subscribe [::subs/loading?])
        fleet-stats               (subscribe [::subs/fleet-stats])
        selected-timespan         (subscribe [::subs/fleet-timespan])
        initial-timespan          (first ts-utils/timespan-options-master)
        fetch-fleet-stats         (fn [timespan]
                                    (let [[from to] (ts-utils/timespan-to-period timespan)]
                                      (dispatch [::events/set-selected-fleet-timespan
                                                 {:timespan-option timespan
                                                  :from            from
                                                  :to              to}])))]
    (fetch-fleet-stats initial-timespan)
    (fn []
      [:div
       [ui/Menu {:width "100%"
                 :borderless true
                 :style {:background-color "#F9FAFB"}}
        [ui/MenuMenu {:position "left"}
         [ts-components/TimeSeriesDropdown {:loading?         @loading?
                                            :default-value    (first ts-utils/timespan-options-master)
                                            :timespan-options ts-utils/timespan-options-master
                                            :on-change-event  ::events/set-selected-fleet-timespan}]]]


       [ui/TabPane
        [FleetStatusTimeSeries @selected-timespan (:availability-stats @fleet-stats)]]])))
