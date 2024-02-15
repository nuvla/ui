(ns sixsq.nuvla.ui.edges.views-timeseries
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edges.subs :as subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.edges.events :as events]
    [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn timestamp+value [data value-key]
  (mapv (fn [d]
          (assoc d :x (:timestamp d)
                   :y (get-in d [:aggregations value-key :value]))) data))

(defn FleetStatusTimeSeries [timespan data]
  (r/with-let [ extra-info-visible? (r/atom false)]

    (let [ts-data             (ts-utils/data->ts-data data)
          [from to] (ts-utils/timespan-to-period timespan)]
      [:div {:style {:display "flex"}}
       [plot/Bar {:data    {:datasets [{:data            (timestamp+value ts-data :virtual-edges-online)
                                        :label           "online"
                                        :backgroundColor "#21d32c88"}
                                       {:data            (mapv (fn [d]
                                                                 (let [offline-edges (get-in d [:aggregations :virtual-edges-offline :value])
                                                                       unknown-edges (get-in d [:aggregations :virtual-edges-unknown-state :value])]
                                                                   (assoc d :x (:timestamp d)
                                                                            :y (+ offline-edges unknown-edges)))) ts-data)
                                        :label           "unknown or offline"
                                        :backgroundColor "#eab81198"}]}

                  :options {:title    "Fleet status"
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
                                           :stacked true
                                           }}
                            :elements {:point {:radius 1}}
                            :onClick  (fn [_evt element _chart]
                                        (when-let [raw-data (.. (first element) -element -$context -raw)]
                                          (js/console.log raw-data)
                                          #_(reset! element-data (js->clj raw-data :keywordize-keys true))
                                          (swap! extra-info-visible? not)))
                            :onHover (fn [evt chartElement]
                                           (let [cursor (if (first chartElement)
                                                          "pointer"
                                                          "default")]
                                             (set! (.. evt -native -target -style -cursor) cursor)))
                            }}]
       (when @extra-info-visible?
         [:div "hello"])])))

(defn FleetTimeSeries []
  (let [tr               (subscribe [::i18n-subs/tr])
        loading?         (subscribe [::subs/loading?])
        fleet-stats      (subscribe [::subs/fleet-stats])
        current-timespan (subscribe [::subs/fleet-timespan])
        initial-timespan (first ts-utils/timespan-options)]
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
        [:div {:style {:max-width 800
                       :margin    "0 auto"}}
         [FleetStatusTimeSeries @current-timespan (:online-status-stats @fleet-stats)]]]])))
