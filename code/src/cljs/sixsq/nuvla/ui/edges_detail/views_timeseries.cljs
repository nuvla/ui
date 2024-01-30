(ns sixsq.nuvla.ui.edges-detail.views-timeseries
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.edges-detail.events :as events]
            [sixsq.nuvla.ui.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.plot :as plot]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def timespan->granularity {"last 15 minutes" "10-seconds"
                            "last hour"       "30-seconds"
                            "last 6 hours"    "3-minutes"
                            "last day"        "10-minutes"
                            "last week"       "1-hours"
                            "last month"      "6-hours"
                            "last 3 months"   "2-days"
                            "last year"       "7-days"})

(defn generate-timestamps [timespan]
  (loop [i          0
         timestamps []]
    (if (> i 200)
      timestamps
      (recur (inc i) (conj timestamps (case timespan
                                        "last 15 minutes" (time/subtract-milliseconds (time/now) (* i 10000))
                                        "last hour" (time/subtract-milliseconds (time/now) (* i 30000))
                                        "last 6 hours" (time/subtract-minutes (time/now) (* i 3))
                                        "last day" (time/subtract-minutes (time/now) (* i 10))
                                        "last week" (time/subtract-minutes (time/now) (* i 60))
                                        "last month" (time/subtract-minutes (time/now) (* i 360))
                                        "last 3 months" (time/subtract-days (time/now) (* i 2))
                                        "last year" (time/subtract-days (time/now) (* i 7))))))))


(defn generate-fake-data-status [timespan]
  (->> (generate-timestamps timespan)
       (mapv (fn [d]
               {:timestamp d
                :metric    "status"
                :status   (rand 1)}))))

(comment
  (generate-fake-data))

(def timespan-options ["last 15 minutes" "last day" "last week" "last month" "last 3 months" "last year"])

(defn timespan-to-period [timespan]
  (let [now (time/now)]
    (case timespan
      "last 15 minutes" [(time/subtract-minutes now 15) now]
      "last hour" [(time/subtract-minutes now 60) now]
      "last 6 hours" [(time/subtract-minutes now 360) now]
      "last day" [(time/subtract-days now 1) now]
      "last week" [(time/subtract-days now 7) now]
      "last month" [(time/subtract-months now 1) now]
      "last 3 months" [(time/subtract-months now 3) now]
      "last year" [(time/subtract-years now 1) now])))

(defn graph-options [timespan {:keys [title y-config plugins]}]
  (let [[from to] (timespan-to-period timespan)]
    {:plugins  (merge {:title {:display  true
                               :text     title
                               :position "top"}}
                      plugins)
     :elements {:point {:radius 1}}

     :scales   {:x {:type  "time"
                    :min   from
                    :max   to
                    :time {:unit (case timespan
                                   (or "last 15 minutes"
                                       "last hour"
                                       "last 6 hours") "minute"
                                   "last day"          "hour"
                                   "last year"         "month"
                                   "day")}
                    :title {:display "true"
                            :text    "Time"}}
                :y y-config}}))

(defn timestamp+percentage [ts-data load-key capacity-key]
  (mapv (fn [d]
          (let [load     (get-in d [:aggregations load-key])
                capacity (get-in d [:aggregations capacity-key])
                percent  (-> (general-utils/percentage load capacity)
                             (general-utils/round-up :n-decimal 0))]
            [(:timestamp d)
             percent]))
        ts-data))

(defn CpuLoadTimeSeries [selected-timespan data]
  (let [ts-data           (-> data
                              (first)
                              (:ts-data))]
    [:div
     [plot/Line {:data    {:datasets [{:data            (timestamp+percentage ts-data :avg-cpu-load :avg-cpu-capacity)
                                       :spanGaps        true
                                       :label           "CPU load"
                                       :backgroundColor "rgb(230, 99, 100, 0.5)"
                                       :borderColor     "rgb(230, 99, 100)"
                                       :borderWidth     1}
                                      {:data            (timestamp+percentage ts-data :avg-cpu-load-1 :avg-cpu-capacity)
                                       :spanGaps        true
                                       :label           "CPU load for the last minute"
                                       :backgroundColor "rgb(99, 230, 178, 0.5019)"
                                       :borderColor     "rgb(99, 230, 178, 0.5019)"
                                       :borderWidth     1}
                                      {:data            (timestamp+percentage ts-data :avg-cpu-load-5 :avg-cpu-capacity)
                                       :spanGaps true
                                       :label           "CPU load for the last 5 minutes"
                                       :backgroundColor "rgb(99, 165, 230, 1)"
                                       :borderColor     "rgb(99, 165, 230)"
                                       :borderWidth     1}]}

                 :options (graph-options selected-timespan {:title    "Average CPU load (%)"
                                                            :y-config {:max   200
                                                                       :min   0
                                                                       :title {:display "true"
                                                                               :text    "Percentage (%)"}}})}]]))

(defn RamUsageTimeSeries [selected-timespan data]
  (let [ts-data (-> data
                    (first)
                    (:ts-data))]
    [:div {:style {:margin-top 35}}
     [plot/Line {:data    {:datasets [{:data            (timestamp+percentage ts-data :avg-ram-used :avg-ram-capacity)
                                       :spanGaps        true
                                       :label           "RAM usage"
                                       :backgroundColor "rgb(230, 99, 100, 0.5)"
                                       :borderColor     "rgb(230, 99, 100)"
                                       :borderWidth     1}]}

                 :options (graph-options selected-timespan {:title    (str "Average RAM usage (%)")
                                                            :y-config {:max   100
                                                                       :min   0
                                                                       :title {:display "true"
                                                                               :text    "Percentage (%)"}}})}]]))

(defn DiskUsageTimeSeries [selected-timespan data]
  (let [disk-load-dataset (fn [{:keys [ts-data dimensions] :as _dataset}]
                            (let [device-name     (:disk.device dimensions)
                                  data-to-display (timestamp+percentage ts-data :avg-disk-used :avg-disk-capacity) ]
                              {:data            data-to-display
                               :spanGaps        true
                               :label           (str "Disk usage (%) for device " device-name)
                               :backgroundColor "rgb(230, 99, 100, 0.5)"
                               :borderColor     "rgb(230, 99, 100)"
                               :borderWidth     1}))]
    [:div
     [plot/Line {:data    {:datasets (mapv disk-load-dataset data)}
                 :options (graph-options selected-timespan {:title    "Average Disk Usage (%)"
                                                            :y-config {:max   100
                                                                       :min   0
                                                                       :title {:display "true"
                                                                               :text    "Percentage (%)"}}})}]]))


(defn interpolate [start end percentage]
  (let [beta (- 1.0 percentage)]
    (mapv #(+ (* %1 beta) (* %2 percentage)) start end)))

(defn color-gradient [color1 color2 percentage]
  (interpolate color1 color2 percentage))

(def red [255 0 0])
(def green [0 255 0])

(defn to-rgb [color-vector]
  (str "rgb(" (str/join "," color-vector) ")"))

(defn NEStatusTimeSeries [selected-timespan data]
  (let [dataset  (->> data
                      (mapv (fn [d]
                              {:x (:timestamp d)
                               :y 1
                               :status (:status d)}) ))]
    [:div
     [plot/Bar {:height  100
                :data    {:datasets [{:data           dataset
                                      :label           "status"
                                      :categoryPercentage 1.0
                                      :barPercentage 1.0
                                      :borderColor (fn [ctx]
                                                     (let [element-status (.. ^Map ctx -raw -status)
                                                           color-gradient (color-gradient red green element-status)]
                                                       (to-rgb color-gradient)))
                                      :backgroundColor (fn [ctx]
                                                         (let [element-status (.. ^Map ctx -raw -status)
                                                               color-gradient (color-gradient red green element-status)]
                                                           (to-rgb color-gradient)))
                                      :borderWidth 1}]}

                :options (graph-options selected-timespan {:title    "NE Status (online/offline)"
                                                           :plugins {:tooltip { :callbacks {:label (fn [tooltipItems _data]
                                                                                                     (str "value: " (.. tooltipItems -raw -status)))}}
                                                                     :legend {:display false}}
                                                           :y-config {:max   1
                                                                      :min   0
                                                                      :ticks {:display false}
                                                                      :title {:display false}}})}]]))

(def colors-palette ["#FFAAA561"
                     "#A8E6CF61"
                     "#FF8B9461"
                     "#DCEDC161"
                     "#EACACB61"
                     "#504A0961"
                     "#D5E1DF61"
                     "#97A39F61"
                     "#E2B3A361"
                     "#A4B5C661"])

(defn NetworkDataTimeSeries [selected-timespan data]
  (r/with-let [selected-intefaces (r/atom [])]
              (let [tr                        (subscribe [::i18n-subs/tr])
                    interfaces                (mapv #(get-in % [:dimensions :network.interface]) data)
                    selected-interfaces-data  (filterv #(contains? (set @selected-intefaces) (get-in % [:dimensions :network.interface])) data)
                    bytes-received-dataset    (fn [interface-data]
                                                (->> (:ts-data interface-data)
                                                     (mapv (fn [d]
                                                             [(:timestamp d)
                                                              (/ (get-in d [:aggregations :bytes-received])
                                                                 1000000)]))))
                    bytes-transmitted-dataset (fn [interface-data]
                                                (->> (:ts-data interface-data)
                                                     (mapv (fn [d]
                                                             [(:timestamp d)
                                                              (* -1 (/ (get-in d [:aggregations :bytes-transmitted])
                                                                       1000000))]))))
                    datasets-to-display      (loop [chart-colors        colors-palette
                                                    interfaces-data     selected-interfaces-data
                                                    datasets-to-display []]
                                               (if (empty? interfaces-data)
                                                 datasets-to-display
                                                 (recur (drop 2 chart-colors)
                                                        (rest interfaces-data)
                                                        (concat datasets-to-display [{:data            (bytes-transmitted-dataset (first interfaces-data))
                                                                                      :label           (str "Transmitted (" (get-in (first interfaces-data) [:dimensions :network.interface]) ")")
                                                                                      :spanGaps        true
                                                                                      :fill            true
                                                                                      :backgroundColor (or (first chart-colors) "gray")
                                                                                      :borderColor     (or (first chart-colors) "gray")
                                                                                      :borderWidth     1}
                                                                                     {:data            (bytes-received-dataset (first interfaces-data))
                                                                                      :label           (str "Received (" (get-in (first interfaces-data) [:dimensions :network.interface]) ")")
                                                                                      :spanGaps        true
                                                                                      :backgroundColor (or (second chart-colors) "gray")
                                                                                      :fill            true
                                                                                      :borderColor     (or (second chart-colors) "gray")
                                                                                      :borderWidth     1}]))))]
                [:div {:style {:display        "flex"
                               :flex-direction "column"
                               :align-items    "end"}}
                 (when (seq interfaces)
                   [ui/Dropdown {:inline          true
                                 :multiple        true
                                 :close-on-change true
                                 :placeholder     (@tr [:choose-network-interface])
                                 :options         (mapv (fn [o] {:key o :text o :value o}) interfaces)
                                 :on-change       (ui-callback/value
                                                    #(reset! selected-intefaces %))}])
                 [plot/Line {:data    {:datasets datasets-to-display}
                             :options (graph-options selected-timespan {:title    (str "Network Traffic (Megabytes)")
                                                                        :y-config {:title {:display "true"
                                                                                           :text    "Megabytes"}}})}]])))

(defn GraphLabel [timespan]
  [ui/Label {:basic true
             :size  "tiny"
             :style {:margin-top "1em"}}
   (str "Data received every " (str/replace (get timespan->granularity timespan) #"-" " "))])

(defn TimeSeries []
  (let [edge-stats            (subscribe [::subs/edge-stats])
        loading?              (subscribe [::subs/loading?])
        selected-timespan     (r/atom (first timespan-options))
        fetch-edge-stats      (fn [timespan]
                                (let [[from to] (timespan-to-period timespan)]
                                  (dispatch [::events/fetch-edge-stats
                                             {:from        from
                                              :to          to
                                              :granularity (get timespan->granularity timespan)
                                              :datasets ["cpu-stats" "disk-stats" "network-stats" "ram-stats" "power-consumption-stats"]}])))]
    (fetch-edge-stats (first timespan-options))
    (fn []
      [ui/TabPane
       [ui/Grid {:columns   2
                 :stackable true
                 :divided   true
                 :celled    "internally"}
        [:div {:style {:display "flex"
                       :width "100%"
                       :justify-content "end"
                       :align-items "center"
                       :padding-bottom "1em"}}
         (when @loading? [ui/Loader {:active true
                                     :inline true
                                     :style {:margin-right 10}
                                     :size   "tiny"}])
         [ui/Menu {:compact true}
          [ui/Dropdown {:item            true
                        :inline          true
                        :close-on-change true
                        :default-value   (first timespan-options)
                        :options         (mapv (fn [o] {:key o :text o :value o}) timespan-options)
                        :on-change       (ui-callback/value
                                           (fn [period]
                                             (do
                                               (reset! selected-timespan period)
                                               (fetch-edge-stats period))))}]]]
        [ui/GridRow
         [ui/GridColumn {:textAlign "center"}
          [CpuLoadTimeSeries @selected-timespan (:cpu-stats @edge-stats)]
          [GraphLabel @selected-timespan]]
         [ui/GridColumn {:textAlign "center"}
          [DiskUsageTimeSeries @selected-timespan (:disk-stats @edge-stats) ]
          [GraphLabel @selected-timespan]]]
        [ui/GridRow
         [ui/GridColumn {:textAlign "center"}
          [NetworkDataTimeSeries @selected-timespan (:network-stats @edge-stats)]
          [GraphLabel @selected-timespan]]
         [ui/GridColumn {:textAlign "center"}
          [RamUsageTimeSeries  @selected-timespan (:ram-stats @edge-stats)]
          [GraphLabel @selected-timespan]]]
        [ui/GridRow
         [ui/GridColumn {:textAlign "center"}
          [NEStatusTimeSeries @selected-timespan (sort-by :timestamp (generate-fake-data-status @selected-timespan))]
          [GraphLabel @selected-timespan]]]]])))
