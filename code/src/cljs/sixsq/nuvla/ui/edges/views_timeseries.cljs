(ns sixsq.nuvla.ui.edges.views-timeseries
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edges.subs :as subs]
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
        stats-by-edge (:availability-by-edge @fleet-stats)
        ts-data       (ts-utils/data->ts-data stats-by-edge)]
    [ui/Card [ui/CardContent
              [ui/CardHeader {:style {:display "flex"
                                      :align-items "center"
                                      :justify-content "space-between"}} [:span "Available NuvlaEdges"]
               [icons/CloseIcon {:link     true
                                 :color "black"
                                 :on-click on-close}]]
              [ui/CardMeta {:style {:font-size "tiny"}}
               (str "on " (time/time->format (:timestamp (first ts-data))))]]
     [ui/CardContent
      (into [ui/CardDescription {:style {:display "flex"
                                         :flex-direction "column"}}]
            (mapv (fn [bucket]
                    (when-let [{:keys [name id]} (info-edge bucket)]
                      [values/AsLink (general-utils/id->uuid id) :page "edges" :label name]))
                  (-> (first ts-data)
                      :aggregations
                      :by-edge
                      :buckets)))]]))

(defn FleetStatusTimeSeries [timespan data]
  (r/with-let [extra-info-visible? (r/atom false)]

    (let [ts-data   (ts-utils/data->ts-data data)
          [from to] (ts-utils/timespan-to-period timespan)]
      [:div {:style {:max-width 800
                     :margin "0 auto"}} #_{:style {:max-width 800
                     :display "flex"
                     :align-items "center"}
               }
       [plot/Bar {:data    {:datasets [{:data            (timestamp+value ts-data :virtual-edges-online)
                                        :label           "available"
                                        :backgroundColor "#21d32c88"}
                                       {:data            (timestamp+value ts-data :virtual-edges-offline)
                                        :label           "unavailable"
                                        :backgroundColor "#eab81198"}]}

                  :options {:plugins  {:title {:text    "Fleet availability"
                                               :display true}
                                       :subtitle {:text "Availability of commissioned NuvlaEdges"
                                                  :display true}}
                            :scales   {:x {:type    "time"
                                           :min     from
                                           :max     to
                                           :time    {:unit (case timespan
                                                             ("last 15 minutes"
                                                               "last hour"
                                                               "last 12 hours") "minute"
                                                             "last day" "hour"
                                                             "last year" "month"
                                                             "day")}
                                           :title   {:display "true"
                                                     :text    "Time"}
                                           :stacked true}
                                       :y {:max     (get-in data [:dimensions :nuvlaedge-count])
                                           :min     0
                                           :title   {:display "true"
                                                     :text    "Number of NuvlaEdges"}
                                           :stacked true}}
                            :elements {:point {:radius 1}}
                            #_#_:onClick  (fn [_evt element _chart]
                                        (when-let [raw-data (js->clj (.. (first element) -element -$context -raw) :keywordize-keys true)]
                                          (let [from        (js/Date. (:timestamp raw-data))
                                                granularity (ts-utils/timespan->granularity timespan)
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

       [:div {:style {:visibility (if @extra-info-visible? "visible" "hidden")
                      :height 150}}
        [OnlineStatsByEdge {:on-close #(reset! extra-info-visible? false)}]]])))

(defn FleetTimeSeries []
  (let [tr               (subscribe [::i18n-subs/tr])
        loading?         (subscribe [::subs/loading?])
        fleet-stats      (subscribe [::subs/fleet-stats])
        current-timespan (subscribe [::subs/fleet-timespan])
        initial-timespan (first ts-utils/timespan-options)]
    (dispatch [::events/set-selected-fleet-timespan initial-timespan])
    (fn []
      [:div [ui/Menu {:width "100%"}
             [ui/MenuMenu {:position "right"}
              [ui/MenuItem
               [:span {:style {:display      "flex"
                               :align-items  "center"
                               :margin-right 5
                               :color        "rgba(40,40,40,.3)"}} (@tr [:showing-data-for])]
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
                                                  (dispatch [::events/set-selected-fleet-timespan timespan])))}]]]]

       [ui/TabPane
        [FleetStatusTimeSeries @current-timespan (:availability-stats @fleet-stats)]]])))
