(ns sixsq.nuvla.ui.edges-detail.views-timeseries
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.edges-detail.events :as events]
            [sixsq.nuvla.ui.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.plot :as plot]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def timespan-options ["last 15 minutes" "last hour" "last 12 hours" "last day" "last week" "last month" "last 3 months" "last year"])

(def timespan->granularity {"last 15 minutes" "1-minutes"
                            "last hour"       "2-minutes"
                            "last 12 hours"   "3-minutes"
                            "last day"        "30-minutes"
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
                                        "last 12 hours" (time/subtract-minutes (time/now) (* i 3))
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
(defn timespan-to-period [timespan]
  (let [now (time/now)]
    (case timespan
      "last 15 minutes" [(time/subtract-minutes now 15) now]
      "last hour" [(time/subtract-minutes now 60) now]
      "last 12 hours" [(time/subtract-minutes now 360) now]
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
                                       "last 12 hours") "minute"
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
                              (:ts-data))
        max-avg-cpu-capacity (->> ts-data
                                  (mapv (fn [d] (get-in d [:aggregations :avg-cpu-capacity])))
                                  (apply max))]
    [:div
     [plot/Line {:data    {:datasets [{:data            (timestamp+percentage ts-data :avg-cpu-load :avg-cpu-capacity)
                                       :spanGaps        true
                                       :label           "CPU load"
                                       :backgroundColor (first plot/default-colors-palette)
                                       :borderColor     (first plot/default-colors-palette)
                                       :borderWidth     1}
                                      {:data            (timestamp+percentage ts-data :avg-cpu-load-1 :avg-cpu-capacity)
                                       :spanGaps        true
                                       :label           "CPU load for the last minute"
                                       :backgroundColor (second plot/default-colors-palette)
                                       :borderColor     (second plot/default-colors-palette)
                                       :borderWidth     1}
                                      {:data            (timestamp+percentage ts-data :avg-cpu-load-5 :avg-cpu-capacity)
                                       :spanGaps true
                                       :label           "CPU load for the last 5 minutes"
                                       :backgroundColor (nth plot/default-colors-palette 2)
                                       :borderColor     (nth plot/default-colors-palette 2)
                                       :borderWidth     1}]}

                 :options (graph-options selected-timespan {:title    "Average CPU load (%)"
                                                            :y-config {:max   (* max-avg-cpu-capacity 100)
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
                                       :backgroundColor (first plot/default-colors-palette)
                                       :borderColor     (first plot/default-colors-palette)
                                       :borderWidth     1}]}

                 :options (graph-options selected-timespan {:title    (str "Average RAM usage (%)")
                                                            :y-config {:max   100
                                                                       :min   0
                                                                       :title {:display "true"
                                                                               :text    "Percentage (%)"}}})}]]))

(defn DiskUsageTimeSeries [selected-timespan data]
  (let [datasets-to-display (loop [chart-colors        plot/default-colors-palette
                                   devices-data        data
                                   datasets-to-display []]
                              (let [{:keys [ts-data dimensions]} (first devices-data)
                                    device-name (:disk.device dimensions)]
                                (if (empty? devices-data)
                                  datasets-to-display
                                  (recur (drop 2 chart-colors)
                                         (rest devices-data)
                                         (conj datasets-to-display {:data            (timestamp+percentage ts-data :avg-disk-used :avg-disk-capacity)
                                                                    :label           (str "Disk usage (%) for device " device-name)
                                                                    :spanGaps        true
                                                                    :backgroundColor (or (first chart-colors) "gray")
                                                                    :borderColor     (or (first chart-colors) "gray")
                                                                    :borderWidth     1})))))]
    [:div
     [plot/Line {:data    {:datasets datasets-to-display}
                 :options (graph-options selected-timespan {:title    "Average Disk Usage (%)"
                                                            :y-config {:max   100
                                                                       :min   0
                                                                       :title {:display "true"
                                                                               :text    "Percentage (%)"}}})}]]))




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
                                                           color-gradient (plot/color-gradient plot/red plot/green element-status)]
                                                       (plot/to-rgb color-gradient)))
                                      :backgroundColor (fn [ctx]
                                                         (let [element-status (.. ^Map ctx -raw -status)
                                                               color-gradient (plot/color-gradient plot/red plot/green element-status)]
                                                           (plot/to-rgb color-gradient)))
                                      :borderWidth 1}]}

                :options (graph-options selected-timespan {:title    "NE Status (online/offline)"
                                                           :plugins {:tooltip { :callbacks {:label (fn [tooltipItems _data]
                                                                                                     (str "value: " (.. tooltipItems -raw -status)))}}
                                                                     :legend {:display false}}
                                                           :y-config {:max   1
                                                                      :min   0
                                                                      :ticks {:display false}
                                                                      :title {:display false}}})}]]))
(defn NetworkDataTimeSeries [selected-timespan data]
  (r/with-let [selected-intefaces (r/atom [])]
              (let [tr                        (subscribe [::i18n-subs/tr])
                    interfaces                (mapv #(get-in % [:dimensions :network.interface]) data)
                    selected-interfaces-data  (filterv #(contains? (set @selected-intefaces) (get-in % [:dimensions :network.interface])) data)
                    bytes-received-dataset    (fn [{:keys [ts-data]}]
                                                (->> ts-data
                                                     (mapv (fn [d]
                                                             [(:timestamp d)
                                                              (/ (get-in d [:aggregations :bytes-received])
                                                                 1000000)]))))
                    bytes-transmitted-dataset (fn [{:keys [ts-data]}]
                                                (->> ts-data
                                                     (mapv (fn [d]
                                                             [(:timestamp d)
                                                              (* -1 (/ (get-in d [:aggregations :bytes-transmitted])
                                                                       1000000))]))))
                    datasets-to-display      (loop [chart-colors        plot/pastel-colors-palette
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

(defn CSVModal [{:keys [on-close]}]
  (r/with-let [form-data (r/atom nil)
               metrics [{:label "CPU Load"
                         :value "cpu-stats"}
                        {:label "Disk Usage"
                         :value "disk-stats"}
                        {:label "Network Traffic"
                         :value "network-stats"}
                        {:label "Ram Usage"
                         :value "ram-stats"}]]
    (js/console.log @form-data)
    [ui/Modal {:close-icon true
               :open       true
               :onClose    on-close}
     [ui/ModalHeader "Export data"]
     [ui/ModalContent
      [ui/ModalDescription
       [:p "Choose the metric and the period for which you wish to export data (in " [:b ".csv"] " format)"]
       [ui/Form
        [:div

         [ui/Header {:as "h4"
                     :attached "top"
                     :style    {:background-color "#00000008"}} "Metric"]
         (into [ui/Segment {:attached true}]
               (for [metric metrics]
                 [ui/FormField
                  [ui/Radio
                   {:label     (:label metric)
                    :name      "radioGroupMetric"
                    :value     (:value metric)
                    :checked   (= (:metric @form-data)
                                  (:value metric))
                    :on-change (fn [_e t]
                                 (swap! form-data assoc :metric (. t -value)))}]]))]
        [:div
         [ui/Header {:as       "h4"
                     :attached "top"
                     :style    {:background-color "#00000008"}} "Period"]
         (into [ui/Segment {:attached true}]
               (for [timespan timespan-options]
                 [ui/FormField
              [ui/Radio
               {:label     timespan
                :name      "radioGroupTimespan"
                :value     timespan
                :checked   (= (:timespan @form-data)
                              timespan)
                :on-change (fn [_e t]
                             (swap! form-data assoc :timespan (. t -value)))}]]))]]]]
     [ui/ModalActions
      [uix/Button {:text     "Export"
                   :icon [icons/i-share]
                   :positive true
                   :disabled (or (not (:metric @form-data))
                                 (not (:timespan @form-data)))
                   :active   true
                   :on-click #(let [[from to] (timespan-to-period (:timespan @form-data))]
                                (dispatch [::events/fetch-edge-stats-csv
                                           {:from        from
                                            :to          to
                                            :granularity (get timespan->granularity (:timespan @form-data))
                                            :dataset     (:metric @form-data)}]))}]]]))

(defn GraphLabel [timespan]
  [ui/Label {:basic true
             :size  "tiny"
             :style {:margin-top "1em"}}
   (str "Per " (str/replace (get timespan->granularity timespan) #"-" " "))])

(defn TimeSeries []
  (let [edge-stats         (subscribe [::subs/edge-stats])
        loading?           (subscribe [::subs/loading?])
        initial-timespan   (first timespan-options)
        selected-timespan  (r/atom initial-timespan)
        csv-modal-visible? (r/atom false)
        fetch-edge-stats   (fn [timespan]
                             (let [[from to] (timespan-to-period timespan)]
                               (dispatch [::events/fetch-edge-stats
                                             {:from        from
                                              :to          to
                                              :granularity (get timespan->granularity timespan)
                                              :datasets ["cpu-stats" "disk-stats" "network-stats" "ram-stats" "power-consumption-stats"]}])))]
    (fetch-edge-stats initial-timespan)
    (fn []
      [:div [ui/Menu {:width "100%"}
             [ui/MenuItem {:icon     [icons/i-share]
                           :item     true
                           :content  "Export data (.csv)"
                           :on-click #(reset! csv-modal-visible? true)}]
             [ui/MenuMenu {:position "right"}
              [ui/MenuItem
               [:span {:style {:display     "flex"
                               :align-items "center"
                               :margin-right 5
                               :color "rgba(40,40,40,.3)"}} "Showing data for the"]
               [ui/Dropdown {:inline          true
                             :loading         @loading?
                             :close-on-change true
                             :default-value   initial-timespan
                             :options         (mapv (fn [o] {:key o :text o :value o}) timespan-options)
                             :on-change       (ui-callback/value
                                                (fn [period]
                                                  (do
                                                    (reset! selected-timespan period)
                                                    (fetch-edge-stats period))))}]]]]
       [ui/TabPane

        [ui/Grid {:columns   2
                  :stackable true
                  :divided   true
                  :celled    "internally"}

         (when @csv-modal-visible?
           [CSVModal {:on-close #(reset! csv-modal-visible? false)}])
         [ui/GridRow
          [ui/GridColumn {:textAlign "center"}
           [CpuLoadTimeSeries @selected-timespan (:cpu-stats @edge-stats)]
           [GraphLabel @selected-timespan]]
          [ui/GridColumn {:textAlign "center"}
           [DiskUsageTimeSeries @selected-timespan (:disk-stats @edge-stats)]
           [GraphLabel @selected-timespan]]]
         [ui/GridRow
          [ui/GridColumn {:textAlign "center"}
           [NetworkDataTimeSeries @selected-timespan (:network-stats @edge-stats)]
           [GraphLabel @selected-timespan]]
          [ui/GridColumn {:textAlign "center"}
           [RamUsageTimeSeries @selected-timespan (:ram-stats @edge-stats)]
           [GraphLabel @selected-timespan]]]
         [ui/GridRow
          [ui/GridColumn {:textAlign "center"}
           [NEStatusTimeSeries @selected-timespan (sort-by :timestamp (generate-fake-data-status @selected-timespan))]
           [GraphLabel @selected-timespan]]]]]])))
