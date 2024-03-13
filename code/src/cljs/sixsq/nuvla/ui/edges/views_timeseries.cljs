(ns sixsq.nuvla.ui.edges.views-timeseries
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edges.subs :as subs]
    [sixsq.nuvla.ui.edges-detail.views-timeseries :as edges-detail.timeseries]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.icons :as icons]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.edges.events :as events]
    [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))

(defn timestamp+value [data value-key]
  (mapv (fn [d]
          (assoc d :x (:timestamp d)
                   :y (get-in d [:aggregations value-key :value]))) data))

(defn info-edge [bucket]
  (when bucket
    {:name       (-> bucket :name)
     :id         (-> bucket :key)
     :avg-online (-> bucket :edge-avg-online :value)}))
(defn OnlineStatsByEdge [{:keys [on-close]}]
  (let [fleet-stats   (subscribe [::subs/fleet-stats])
        tr            (subscribe [::i18n-subs/tr])
        stats-by-edge (:availability-by-edge @fleet-stats)
        ts-data       (ts-utils/data->ts-data stats-by-edge)
        n             10
        least-available-nuvlaedges (->> (first ts-data)
                                        :aggregations
                                        :by-edge
                                        :buckets
                                        (filter (fn [{:keys [edge-avg-online]}]
                                                  (< (:value edge-avg-online) 1)))
                                        (sort-by (comp :value :edge-avg-online))
                                        (take n))]
    [ui/Card [ui/CardContent
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
      [ui/CardDescription
       (if (seq least-available-nuvlaedges)
         (into [ui/Table {:basic "very"}
                [ui/TableHeader
                 [ui/TableRow
                  [ui/TableHeaderCell (str/capitalize (@tr [:name]))]
                  [ui/TableHeaderCell  {:textAlign "right"} (str (@tr [:availability]) " (%)")]]]]
               (mapv (fn [bucket]
                       (when-let [{:keys [name id avg-online]} (info-edge bucket)]
                         [ui/TableRow
                          [ui/TableCell [values/AsLink (str (general-utils/id->uuid id) "?edges-detail-tab=historical-data") :page "edges" :label name]]
                          [ui/TableCell {:textAlign "right"} (int (* 100 avg-online))]]))
                     least-available-nuvlaedges))
         [:span (@tr [:no-data-to-show])])]]]))

(defn FleetStatusTimeSeries [{:keys [timespan-option] :as timespan} data]
  (r/with-let [extra-info-visible? (r/atom false)]
    (let [tr      (subscribe [::i18n-subs/tr])
          ts-data (ts-utils/data->ts-data data)
          [from to] (if-not (= "custom period" timespan-option)
                      (ts-utils/timespan-to-period (:timespan-option timespan))
                      [(:from timespan) (:to timespan)])]
      [ui/Grid {:padded true
                :columns 2}
       [ui/GridRow {:centered true}
        [ui/GridColumn {:width 10
                        :textAlign "center"}
         [plot/Bar {:data    {:datasets [{:data            (timestamp+value ts-data :virtual-edges-online)
                                          :label           (@tr [:available])
                                          :backgroundColor "#21d32c88"}
                                         {:data            (timestamp+value ts-data :virtual-edges-offline)
                                          :label           (@tr [:unavailable])
                                          :backgroundColor "#eab81198"}]}

                    :options {:plugins  {:title    {:text     (@tr [:fleet-availability])
                                                    :display true}
                                         :subtitle {:text    (@tr [:availability-commissioned-nuvlaedges])
                                                    :display true}}
                              :scales   {:x {:type    "time"
                                             :min     from
                                             :max     to
                                             :grid    {:display false}
                                             :time    {:unit (case (:timespan-option timespan)
                                                               ("last 15 minutes"
                                                                 "last hour"
                                                                 "last 6 hours") "minute"
                                                               "last day" "hour"
                                                               "last year" "month"
                                                               "day")}
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
                                          (when-let [raw-data (js->clj (.. (first element) -element -$context -raw) :keywordize-keys true)]
                                            (let [from        (js/Date. (:timestamp raw-data))
                                                  granularity (ts-utils/granularity-for-timespan timespan)
                                                  to          (ts-utils/add-time from granularity)]
                                              (dispatch [::events/fetch-fleet-stats {:from        from
                                                                                     :to          to
                                                                                     :granularity granularity
                                                                                     :dataset     ["availability-by-edge"]}])
                                              (reset! extra-info-visible? true))))
                              :onHover  (fn [evt chartElement]
                                          (let [cursor (if (first chartElement)
                                                         "pointer"
                                                         "default")]
                                            (set! (.. evt -native -target -style -cursor) cursor)))}}]
         [edges-detail.timeseries/GraphLabel timespan]]

        [ui/GridColumn {:width 4}
         [:div {:style {:visibility (if @extra-info-visible? "visible" "hidden")}}
          [OnlineStatsByEdge {:on-close #(reset! extra-info-visible? false)}]]]]])))

(defn FleetTimeSeries []
  (let [tr                        (subscribe [::i18n-subs/tr])
        loading?                  (subscribe [::subs/loading?])
        fleet-stats               (subscribe [::subs/fleet-stats])
        selected-timespan         (subscribe [::subs/fleet-timespan])
        initial-timespan          (first ts-utils/timespan-options)
        currently-selected-option (r/atom initial-timespan)
        custom-timespan           (r/atom {})
        fetch-fleet-stats         (fn [timespan]
                                    (let [[from to] (ts-utils/timespan-to-period timespan)]
                                      (dispatch [::events/set-selected-fleet-timespan
                                                 {:timespan-option timespan
                                                  :from            from
                                                  :to              to}])))]
    (fetch-fleet-stats initial-timespan)
    (fn []
      [:div [ui/Menu {:width "100%"}
             [ui/MenuMenu {:position "left"}
              [ui/MenuItem {:style {:padding-top 5
                                    :padding-bottom 5
                                    :padding-left 16
                                    :height 45}}
               [:span {:style {:display      "flex"
                               :align-items  "center"
                               :margin-right 5}} (@tr [:showing-data-for])]
               [ui/Dropdown {:inline          true
                             :style           {:min-width       120
                                               :display         "flex"
                                               :justify-content "space-between"}
                             :loading         @loading?
                             :close-on-change true
                             :default-value   initial-timespan
                             :options         (mapv (fn [o] {:key o :text (@tr [(ts-utils/format-option o)]) :value o}) ts-utils/timespan-options)
                             :on-change       (ui-callback/value
                                                (fn [timespan]
                                                  (reset! currently-selected-option timespan)
                                                  (when-not (= "custom period" timespan)
                                                    (let [[from to] (ts-utils/timespan-to-period timespan)]
                                                      (reset! currently-selected-option timespan)
                                                      (reset! custom-timespan {})
                                                      (dispatch [::events/set-selected-fleet-timespan {:timespan-option timespan
                                                                                                       :from from
                                                                                                       :to to}])))))}]
               [:div {:style {:display     "flex"
                              :margin-left 10
                              :visibility  (if (= "custom period" @currently-selected-option)
                                             "visible"
                                             "hidden")}}
                [edges-detail.timeseries/CustomPeriodSelector @custom-timespan
                 {:on-change-fn-from #(do (swap! custom-timespan assoc :from %)
                                          (when (:to @custom-timespan)
                                            (dispatch [::events/set-selected-fleet-timespan {:from %
                                                                                             :to (:to @custom-timespan)
                                                                                             :timespan-option "custom period"}])))
                  :on-change-fn-to   #(do (swap! custom-timespan assoc :to %)
                                          (when (:from @custom-timespan)
                                            (dispatch [::events/set-selected-fleet-timespan {:from (:from @custom-timespan)
                                                                                             :to %
                                                                                             :timespan-option "custom period"}])))}]]]]]


       [ui/TabPane
        [FleetStatusTimeSeries @selected-timespan (:availability-stats @fleet-stats)]]])))
