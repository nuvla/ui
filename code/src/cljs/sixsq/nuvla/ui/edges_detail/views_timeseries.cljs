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
            [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def timespan-options ["last 15 minutes" "last hour" "last 12 hours" "last day" "last week" "last month" "last 3 months" "last year"])

(defn data->ts-data [data] (-> data first :ts-data))

(defn graph-options [timespan {:keys [title y-config plugins]}]

  (let [[from to] (ts-utils/timespan-to-period timespan)]
    {:plugins  (merge {:title {:display  true
                               :text     title
                               :position "top"}}
                      plugins)
     :elements {:point {:radius 1}}

     :scales   {:x {:type  "time"
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
                :y y-config}}))

(defn timestamp+percentage [ts-data load-key capacity-key]
  (mapv (fn [d]
          (let [load     (get-in d [:aggregations load-key :value])
                capacity (get-in d [:aggregations capacity-key :value])
                percent  (-> (general-utils/percentage load capacity)
                             (general-utils/round-up :n-decimal 0))]
            (assoc d :x (:timestamp d)
                     :y percent)))
        ts-data))

(defn CpuLoadTimeSeries [selected-timespan data]
  (let [tr      (subscribe [::i18n-subs/tr])
        ts-data (data->ts-data data)]
    [:div
     [plot/Line {:updateMode "none"
                 :data       {:datasets [{:data            (timestamp+percentage ts-data :avg-cpu-load :avg-cpu-capacity)
                                          :label           (@tr [:load])
                                          :backgroundColor (first plot/default-colors-palette)
                                          :borderColor     (first plot/default-colors-palette)
                                          :borderWidth     1}
                                         {:data            (timestamp+percentage ts-data :avg-cpu-load-1 :avg-cpu-capacity)
                                          :label           (@tr [:load-1-m])
                                          :backgroundColor (second plot/default-colors-palette)
                                          :borderColor     (second plot/default-colors-palette)
                                          :borderWidth     1}
                                         {:data            (timestamp+percentage ts-data :avg-cpu-load-5 :avg-cpu-capacity)
                                          :label           (@tr [:load-5-m])
                                          :backgroundColor (nth plot/default-colors-palette 2)
                                          :borderColor     (nth plot/default-colors-palette 2)
                                          :borderWidth     1}]}

                 :options    (graph-options selected-timespan {:title    (str (@tr [:average-cpu-load]) " (%)")
                                                               :y-config {:max   100
                                                                          :min   0
                                                                          :title {:display "true"
                                                                                  :text    "Percentage (%)"}}})}]]))

(defn RamUsageTimeSeries [selected-timespan data]
  (let [tr      (subscribe [::i18n-subs/tr])
        ts-data (data->ts-data data)]
    [:div {:style {:margin-top 35}}
     [plot/Line {:updateMode "none"
                 :data       {:datasets [{:data            (timestamp+percentage ts-data :avg-ram-used :avg-ram-capacity)
                                          :backgroundColor (first plot/default-colors-palette)
                                          :borderColor     (first plot/default-colors-palette)
                                          :borderWidth     1}]}
                 :options    (graph-options selected-timespan {:title    (str (@tr [:average-ram-usage]) " (%)")
                                                               :plugins  {:legend {:display false}}
                                                               :y-config {:max   100
                                                                          :min   0
                                                                          :title {:display "true"
                                                                                  :text    "Percentage (%)"}}})}]]))

(defn DiskUsageTimeSeries [selected-timespan data]
  (let [tr                  (subscribe [::i18n-subs/tr])
        datasets-to-display (loop [chart-colors        plot/default-colors-palette
                                   devices-data        data
                                   datasets-to-display []]
                              (let [{:keys [ts-data dimensions]} (first devices-data)
                                    device-name (:disk.device dimensions)]
                                (if (empty? devices-data)
                                  datasets-to-display
                                  (recur (drop 1 chart-colors)
                                         (rest devices-data)
                                         (conj datasets-to-display {:data            (timestamp+percentage ts-data :avg-disk-used :avg-disk-capacity)
                                                                    :label           device-name
                                                                    :backgroundColor (or (first chart-colors) "gray")
                                                                    :borderColor     (or (first chart-colors) "gray")
                                                                    :borderWidth     1})))))]
    [:div
     [plot/Line {:updateMode "none"
                 :data       {:datasets datasets-to-display}
                 :options    (graph-options selected-timespan {:title    (str (@tr [:average-disk-usage]) " (%)")
                                                               :y-config {:max   100
                                                                          :min   0
                                                                          :title {:display "true"
                                                                                  :text    (str (@tr [:percentage]) " (%)")}}})}]]))




(defn NEStatusTimeSeries [selected-timespan data]
  (let [tr      (subscribe [::i18n-subs/tr])
        ts-data (data->ts-data data)
        dataset (->> ts-data
                     (mapv (fn [d]
                             {:x      (:timestamp d)
                              :y      1
                              :status (-> d :aggregations :avg-online :value)})))]
    [:div
     [plot/Bar {:updateMode "none"
                :height     100
                :data       {:datasets (if (seq ts-data)
                                         [{:data               dataset
                                           :label              "status"
                                           :categoryPercentage 1.0
                                           :barPercentage      1.0
                                           :borderColor (fn [ctx]
                                                              (if-let [element-status (.. ^Map ctx -raw -status)]
                                                                (let [color-gradient (plot/color-gradient element-status)]
                                                                  (plot/to-rgb color-gradient))
                                                                "gray"))
                                          :backgroundColor (fn [ctx]
                                                                  (if-let [element-status (.. ^Map ctx -raw -status)]
                                                                    (let [color-gradient (plot/color-gradient element-status)]
                                                                      (plot/to-rgb color-gradient))
                                                                    "gray"))
                                           :borderWidth        1}]
                                         [])}

                :options    (graph-options selected-timespan {:title    (str (@tr [:nuvlaedge-status]) " (" (@tr [:online]) "/" (@tr [:offline]) ")")
                                                              :plugins  {:tooltip {:callbacks {:label (fn [tooltipItems _data]
                                                                                                        (when-let [status (.. ^Map tooltipItems -raw -status)]
                                                                                                          (str "online: " (* (general-utils/round-up status :n-decimal 2) 100) "%")))}}
                                                                         :legend  {:display false}}
                                                              :y-config {:max   1
                                                                         :min   0
                                                                         :grid  {:display false}
                                                                         :ticks {:display false}
                                                                         :title {:display false}}})}]]))
(defn NetworkDataTimeSeries [_selected-timespan data]
  (when data
    (let [interfaces         (mapv #(get-in % [:dimensions :network.interface]) data)
          initial-interface  (or ((set interfaces) "eth0") (first interfaces))
          selected-intefaces (r/atom [initial-interface])]
      (fn [selected-timespan data]
        (let [tr                        (subscribe [::i18n-subs/tr])
              interfaces                (mapv #(get-in % [:dimensions :network.interface]) data)
              selected-interfaces-data  (filterv #(contains? (set @selected-intefaces) (get-in % [:dimensions :network.interface])) data)
              bytes-received-dataset    (fn [{:keys [ts-data]}]
                                          (->> ts-data
                                               (mapv (fn [d]
                                                       [(:timestamp d)
                                                        (/ (get-in d [:aggregations :bytes-received :value])
                                                           1000000)]))))
              bytes-transmitted-dataset (fn [{:keys [ts-data]}]
                                          (->> ts-data
                                               (mapv (fn [d]
                                                       [(:timestamp d)
                                                        (* -1 (/ (get-in d [:aggregations :bytes-transmitted :value])
                                                                 1000000))]))))
              datasets-to-display       (loop [chart-colors        plot/pastel-colors-palette
                                               interfaces-data     selected-interfaces-data
                                               datasets-to-display []]
                                          (if (empty? interfaces-data)
                                            datasets-to-display
                                            (recur (drop 2 chart-colors)
                                                   (rest interfaces-data)
                                                   (concat datasets-to-display [{:data            (bytes-transmitted-dataset (first interfaces-data))
                                                                                 :label           (str (@tr [:transmitted]) " (" (get-in (first interfaces-data) [:dimensions :network.interface]) ")")
                                                                                 :fill            true
                                                                                 :backgroundColor (or (first chart-colors) "gray")
                                                                                 :borderColor     (or (first chart-colors) "gray")
                                                                                 :borderWidth     1}
                                                                                {:data            (bytes-received-dataset (first interfaces-data))
                                                                                 :label           (str (@tr [:received]) " (" (get-in (first interfaces-data) [:dimensions :network.interface]) ")")
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
                           :default-value   [initial-interface]
                           :placeholder     (@tr [:choose-network-interface])
                           :options         (mapv (fn [o] {:key o :text o :value o}) interfaces)
                           :on-change       (ui-callback/value
                                              #(reset! selected-intefaces %))}])
           [plot/Line {:updateMode "none"
                       :data       {:datasets datasets-to-display}
                       :options    (graph-options selected-timespan {:title    (str (@tr [:network-traffic]) " (" (@tr [:megabytes]) ")")
                                                                     :y-config {:title {:display "true"
                                                                                        :text    (@tr [:megabytes])}}})}]])))))

(defn ExportDataModal [{:keys [on-close]}]
  (r/with-let [form-data (r/atom nil)]
    (fn []
      (let [tr      (subscribe [::i18n-subs/tr])
            metrics [{:label   (@tr [:average-cpu-load])
                        :value "cpu-stats"}
                       {:label (@tr [:average-disk-usage])
                        :value "disk-stats"}
                       {:label (@tr [:network-traffic])
                        :value "network-stats"}
                       {:label (@tr [:average-ram-usage])
                        :value "ram-stats"}
                       {:label (str (@tr [:nuvlaedge-status]) " (" (@tr [:online]) "/" (@tr [:offline]) ")")
                        :value "availability-stats"}]]
        [ui/Modal {:close-icon true
                   :open       true
                   :onClose    on-close}
         [ui/ModalHeader (@tr [:export-data])]
         [ui/ModalContent
          [ui/ModalDescription
           [:p (@tr [:choose-metric-period])]
           [ui/Form
            [:div

             [ui/Header {:as       "h4"
                         :attached "top"
                         :style    {:background-color "#00000008"}} (@tr [:metric])]
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
                         :style    {:background-color "#00000008"}} (str/capitalize (@tr [:period]))]
             (into [ui/Segment {:attached true}]
                   (for [option timespan-options]
                     [ui/FormField
                      [ui/Radio
                       {:label     (@tr [(ts-utils/format-option option)])
                        :name      "radioGroupTimespan"
                        :value     option
                        :checked   (= (:timespan @form-data)
                                      option)
                        :on-change (fn [_e t]
                                     (swap! form-data assoc :timespan (. t -value)))}]]))]]]]
         [ui/ModalActions
          [uix/Button {:text     (@tr [:export])
                       :icon     icons/i-export
                       :positive true
                       :disabled (or (not (:metric @form-data))
                                     (not (:timespan @form-data)))
                       :active   true
                       :on-click #(let [[from to] (ts-utils/timespan-to-period (:timespan @form-data))]
                                    (dispatch [::events/fetch-edge-stats-csv
                                               {:from        from
                                                :to          to
                                                :granularity (get ts-utils/timespan->granularity (:timespan @form-data))
                                                :dataset     (:metric @form-data)}]))}]]]))))

(defn GraphLabel [timespan]
  (let [tr (subscribe [::i18n-subs/tr])
        [number unit] (str/split (get ts-utils/timespan->granularity timespan) #"-")]
    [ui/Label {:basic true
               :size  "tiny"
               :style {:margin-top "1em"}}
     (str "Per " number " " (@tr [(keyword unit)]))]))
(defn TimeSeries []
  (let [tr                    (subscribe [::i18n-subs/tr])
        edge-stats            (subscribe [::subs/edge-stats])
        loading?              (subscribe [::subs/loading?])
        initial-timespan      (first timespan-options)
        selected-timespan     (subscribe [::subs/timespan])
        export-modal-visible? (r/atom false)
        datasets              ["cpu-stats" "disk-stats" "network-stats" "ram-stats" "power-consumption-stats" "availability-stats"]
        fetch-edge-stats      (fn [timespan]
                                (dispatch [::events/set-selected-timespan
                                           timespan
                                           (get ts-utils/timespan->granularity timespan)
                                           datasets]))]

    (fetch-edge-stats (first timespan-options))
    (fn []
      [:div [ui/Menu {:width "100%"}
             [ui/MenuItem {:icon     icons/i-export
                           :content  (str (@tr [:export-data]) " (.csv)")
                           :on-click #(reset! export-modal-visible? true)}]
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
                             :options         (mapv (fn [o] {:key o :text (@tr [(ts-utils/format-option o)]) :value o}) timespan-options)
                             :on-change       (ui-callback/value
                                                (fn [timespan]
                                                  (dispatch [::events/set-selected-timespan timespan (ts-utils/timespan->granularity timespan) datasets])))}]]]]
       [ui/TabPane
        [ui/Grid {:columns   2
                  :stackable true
                  :divided   true
                  :celled    "internally"}

         (when @export-modal-visible?
           [ExportDataModal {:on-close #(reset! export-modal-visible? false)}])
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
         [ui/GridRow {:columns 1
                      :centered true}
          [ui/GridColumn {:textAlign "center"
                          :width 8}
           [NEStatusTimeSeries @selected-timespan (:availability-stats @edge-stats)]
           [GraphLabel @selected-timespan]]]]]])))
