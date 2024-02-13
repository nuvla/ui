(ns sixsq.nuvla.ui.edges.views-timeseries
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.edges.subs :as subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.edges.events :as events]
    [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn graph-options [timespan {:keys [title y-config plugins x-config]}]
  (let [[from to] (ts-utils/timespan-to-period timespan)]
    {:plugins  (merge {:title {:display  true
                               :text     title
                               :position "top"}}
                      plugins)
     :elements {:point {:radius 1}}
     :scales   {:x (merge {:type  "time"
                                   :min   from
                                   :max   to
                                   :time  {:unit (case timespan
                                                   ("last 15 minutes"
                                                     "last hour"
                                                     "last 12 hours") "minute"
                                                   "last day" "hour"
                                                   "last year" "month"
                                                   "day")}
                                   :title {:display "true"
                                           :text    "Time"}}
                          x-config)
                :y y-config}}))

(defn FleetStatusTimeSeries [timespan data]
  (let [data (ts-utils/data->ts-data data)
        online-dataset (->> data
                            (map (fn [d]
                                   (let [avg-value (get-in d [:aggregations :adjusted-avg-avg-online :value])]
                                     (assoc d :x (:timestamp d)
                                              :y (* (general-utils/round-up avg-value :n-decimal 2) 100))))))
        offline-dataset (->> data
                             (map (fn [d]
                                    (let [avg-value (get-in d [:aggregations :adjusted-avg-avg-online :value])
                                          online-percentage (* (general-utils/round-up avg-value :n-decimal 2) 100)]
                                      (assoc d :x (:timestamp d)
                                               :y (- 100 online-percentage))))))]
    [plot/Bar {:data    {:datasets [{:data            online-dataset
                                     :label           "online"
                                     :backgroundColor "green"}
                                    {:data            offline-dataset
                                     :label           "offline"
                                     :backgroundColor "red"}]}

               :options (graph-options timespan {:title    "Fleet uptime percentage over time"
                                                          :x-config {:stacked true}
                                                          :y-config {:max     100
                                                                     :min     0
                                                                     :title   {:display "true"
                                                                               :text    "Percentage (%)"}
                                                                     :stacked true
                                                                     }})}]))

(defn FleetTimeSeries []
  (let [tr                     (subscribe [::i18n-subs/tr])
        loading?                (subscribe [::subs/loading?])
        fleet-stats            (subscribe [::subs/fleet-stats])
        current-timespan       (subscribe [::subs/fleet-timespan])
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
