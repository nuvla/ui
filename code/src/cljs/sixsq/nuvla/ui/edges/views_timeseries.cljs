(ns sixsq.nuvla.ui.edges.views-timeseries
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.edges.subs :as subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.edges.events :as events]
    [sixsq.nuvla.ui.utils.timeseries :as ts-utils]))

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

(defn FleetStatusTimeSeries []
  (let [nuvlaboxes             (subscribe [::subs/nuvlaboxes])
        fleet-stats            (subscribe [::subs/fleet-stats])
        current-timespan       (subscribe [::subs/fleet-timespan])
        current-cluster (subscribe [::subs/nuvlabox-cluster])
        selected-nbs    (if @current-cluster
                          (for [target-nb-id (concat (:nuvlabox-managers @current-cluster)
                                                     (:nuvlabox-workers @current-cluster))]
                            (into {} (get (group-by :id (:resources @nuvlaboxes)) target-nb-id)))
                          (:resources @nuvlaboxes))
        fetch-fleet-stats      (fn [timespan]
                                (dispatch [::events/fetch-fleet-stats
                                           {:timespan timespan
                                            :granularity (get ts-utils/timespan->granularity timespan)
                                            :datasets ["online-status-stats"]
                                            :nuvlaedge-ids (mapv :id selected-nbs)}]))
        data  (ts-utils/data->ts-data (:online-status-stats @fleet-stats))
        online-dataset (->> data
                            (map (fn [d]
                                   (let [avg-value (get-in d [:aggregations :avg-avg-online :value])]
                                     (assoc d :x (:timestamp d)
                                              :y (* (general-utils/round-up avg-value :n-decimal 2) 100))))))
        offline-dataset (->> data
                             (map (fn [d]
                                    (let [avg-value (get-in d [:aggregations :avg-avg-online :value])
                                          online-percentage (* (general-utils/round-up avg-value :n-decimal 2) 100)]
                                      (assoc d :x (:timestamp d)
                                               :y (- 100 online-percentage))))))]
    (fetch-fleet-stats "last 15 minutes")
    (fn []
      [ui/TabPane
       [:div {:style {:max-width 800
                      :margin    "0 auto"}}
        [plot/Bar {:updateMode "none"
                   :data       {:datasets [{:data            online-dataset
                                            :label           "online"
                                            :backgroundColor "green"}
                                           {:data            offline-dataset
                                            :label           "offline"
                                            :backgroundColor "red"}]}

                   :options    (graph-options @current-timespan {:title    "Fleet uptime percentage over time"
                                                                 :x-config {:stacked true}
                                                                 :y-config {:max     100
                                                                            :min     0
                                                                            :title   {:display "true"
                                                                                      :text    "Percentage (%)"}
                                                                            :stacked true
                                                                            }})}]]])))
