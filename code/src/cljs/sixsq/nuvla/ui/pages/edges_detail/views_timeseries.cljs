(ns sixsq.nuvla.ui.pages.edges-detail.views-timeseries
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.edges-detail.events :as events]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.plot :as plot]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
            [sixsq.nuvla.ui.utils.timeseries-components :as ts-components]
            [sixsq.nuvla.ui.utils.timeseries-components]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn graph-options [tr {:keys [timespan-option] :as timespan} {:keys [title y-config plugins x-config]}]
  (let [[from to] (if-not (= "custom period" timespan-option)
                    (ts-utils/timespan-to-period (:timespan-option timespan))
                    [(:from timespan) (:to timespan)])]
    {:plugins  (merge {:title {:display  true
                               :text     title
                               :position "top"}}
                      plugins)
     :elements {:point {:radius 1}}

     :scales   {:x (merge {:type  "time"
                           :min   from
                           :max   to
                           :time  {:unit (case (:timespan-option timespan)
                                           ("last 15 minutes"
                                             "last hour"
                                             "last 6 hours") "minute"
                                           "last day" "hour"
                                           "last year" "month"
                                           "day")}
                           :title {:display "true"
                                   :text    (@tr [:time])}}
                          x-config)
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
        ts-data (ts-utils/data->timeseries-data data)]
    [:div
     [plot/Line {:updateMode "none"
                 :data       {:datasets [{:data            (timestamp+percentage ts-data :avg-cpu-load :avg-cpu-capacity)
                                          :label           (@tr [:last-15m])
                                          :backgroundColor (first plot/default-colors-palette)
                                          :borderColor     (first plot/default-colors-palette)
                                          :borderWidth     1}
                                         {:data            (timestamp+percentage ts-data :avg-cpu-load-1 :avg-cpu-capacity)
                                          :label           (@tr [:last-1m])
                                          :backgroundColor (second plot/default-colors-palette)
                                          :borderColor     (second plot/default-colors-palette)
                                          :borderWidth     1}
                                         {:data            (timestamp+percentage ts-data :avg-cpu-load-5 :avg-cpu-capacity)
                                          :label           (@tr [:last-5m])
                                          :backgroundColor (nth plot/default-colors-palette 2)
                                          :borderColor     (nth plot/default-colors-palette 2)
                                          :borderWidth     1}]}

                 :options    (graph-options tr selected-timespan {:title    (str (@tr [:average-cpu-load]) " (%)")
                                                                  :y-config {:max   200
                                                                             :min   0
                                                                             :title {:display "true"
                                                                                     :text    (str (@tr [:percentage]) " (%)")}}})}]]))

(defn RamUsageTimeSeries [selected-timespan data]
  (let [tr      (subscribe [::i18n-subs/tr])
        ts-data (ts-utils/data->timeseries-data data)]
    [:div {:style {:margin-top 35}}
     [plot/Line {:updateMode "none"
                 :data       {:datasets [{:data            (timestamp+percentage ts-data :avg-ram-used :avg-ram-capacity)
                                          :backgroundColor (first plot/default-colors-palette)
                                          :borderColor     (first plot/default-colors-palette)
                                          :borderWidth     1}]}
                 :options    (graph-options tr selected-timespan {:title    (str (@tr [:average-ram-usage]) " (%)")
                                                                  :plugins  {:legend {:display false}}
                                                                  :y-config {:max   100
                                                                             :min   0
                                                                             :title {:display "true"
                                                                                     :text    (str (@tr [:percentage]) " (%)")}}})}]]))

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
                 :options    (graph-options tr selected-timespan {:title    (str (@tr [:average-disk-usage]) " (%)")
                                                               :y-config {:max   100
                                                                          :min   0
                                                                          :title {:display "true"
                                                                                  :text    (str (@tr [:percentage]) " (%)")}}})}]]))




(defn NEStatusTimeSeries [selected-timespan data]
  (let [tr      (subscribe [::i18n-subs/tr])
        ts-data (ts-utils/data->timeseries-data data)
        dataset (->> ts-data
                     (mapv (fn [d]
                             {:x      (:timestamp d)
                              :y      (if (seq (:aggregations d)) 1 nil)
                              :status (-> d :aggregations :avg-online :value)})))]
    [:div#nuvlaedge-status
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

                :options    (graph-options tr selected-timespan {:title (@tr [:nuvlaedge-availability])
                                                                 :plugins {:tooltip {:callbacks {:label (fn [tooltipItems _data]
                                                                                                          (when-let [status (.. ^Map tooltipItems -raw -status)]
                                                                                                            (str "available: " (* (general-utils/round-up status :n-decimal 2) 100) "%")))}}
                                                                           :legend  {:display false}}
                                                                 :y-config {:max   1
                                                                            :min   0
                                                                            :grid  {:display false}
                                                                            :ticks {:display false}
                                                                            :title {:display false}}
                                                                 :x-config {:ticks {:source "data"}}})}]]))
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
                       :options    (graph-options tr selected-timespan {:title    (str (@tr [:network-traffic]) " (" (@tr [:megabytes]) ")")
                                                                     :y-config {:title {:display "true"
                                                                                        :text    (@tr [:megabytes])}}})}]])))))

(defn ExportDataModal [{:keys [on-close]}]
  (r/with-let [state (r/atom {:form-data               {}
                              :custom-period-selected? false})]
    (fn []
      (let [tr      (subscribe [::i18n-subs/tr])
            metrics [{:label (@tr [:average-cpu-load])
                      :value "cpu-stats"}
                     {:label (@tr [:average-disk-usage])
                      :value "disk-stats"}
                     {:label (@tr [:network-traffic])
                      :value "network-stats"}
                     {:label (@tr [:average-ram-usage])
                      :value "ram-stats"}
                     {:label (@tr [:nuvlaedge-availability])
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
                   (for [{:keys [label value]} metrics]
                     [ui/FormField
                      [ui/Radio
                       {:label     label
                        :name      "radioGroupMetric"
                        :value     value
                        :checked   (= (get-in @state [:form-data :metric])
                                      value)
                        :on-change (fn [_e t]
                                     (swap! state assoc-in [:form-data :metric] (. t -value)))}]]))]
            [:div
             [ui/Header {:as       "h4"
                         :attached "top"
                         :style    {:background-color "#00000008"}}
              (str/capitalize (@tr [:period]))]
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
                       :disabled (or (not (-> @state :form-data :metric))
                                     (not (-> @state :form-data :from))
                                     (not (-> @state :form-data :to)))
                       :active   true
                       :on-click #(let [{:keys [from to metric] :as form-data} (:form-data @state)]
                                    (dispatch [::events/fetch-edge-stats-csv
                                               {:from        from
                                                :to          to
                                                :granularity (ts-utils/granularity-for-timespan form-data)
                                                :dataset     metric}]))}]]]))))

(defn GraphLabel [timespan]
  (let [tr (subscribe [::i18n-subs/tr])
        [number unit] (str/split (ts-utils/granularity-for-timespan timespan) #"-")]
    [ui/Label {:basic true
               :size  "tiny"
               :style {:margin-top "1em"}}
     (str "Per " number " " (@tr [(keyword unit)]))]))

(defn TimeSeries []
  (let [tr                        (subscribe [::i18n-subs/tr])
        edge-stats                (subscribe [::subs/edge-stats])
        loading?                  (subscribe [::subs/loading?])
        selected-timespan         (subscribe [::subs/timespan])
        export-modal-visible?     (r/atom false)
        fetch-edge-stats          (fn [timespan]
                                    (let [[from to] (ts-utils/timespan-to-period timespan)]
                                      (dispatch [::events/set-selected-timespan {:timespan-option timespan
                                                                                 :from from
                                                                                 :to to}])))]
    (fetch-edge-stats (first ts-utils/timespan-options))
    (fn []
      [:div [ui/Menu {:width "100%"
                      :borderless true
                      :style  {:background-color "#F9FAFB"}}
             [ui/MenuMenu {:position "left"}
              [ts-components/TimeSeriesDropdown {:loading? @loading?
                                                 :default-value (first ts-utils/timespan-options)
                                                 :timespan-options ts-utils/timespan-options
                                                 :on-change-event ::events/set-selected-timespan}]]
             [ui/MenuItem {:icon     icons/i-export
                           :position "right"
                           :content  (str (@tr [:export-data]) " (.csv)")
                           :on-click #(reset! export-modal-visible? true)}]]

       [ui/TabPane
        [ui/Grid {:columns   2
                  :stackable true}
         (when @export-modal-visible?
           [ExportDataModal {:on-close #(reset! export-modal-visible? false)}])
         [ui/GridRow
          [ui/GridColumn {:textAlign "center"}
           [ui/Segment {:style  {:background-color "#F9FAFB"}}
            [CpuLoadTimeSeries @selected-timespan (:cpu-stats @edge-stats)]
            [GraphLabel @selected-timespan]]]
          [ui/GridColumn {:textAlign "center"}
           [ui/Segment {:style  {:background-color "#F9FAFB"}}
            [DiskUsageTimeSeries @selected-timespan (:disk-stats @edge-stats)]
            [GraphLabel @selected-timespan]]]]
         [ui/GridRow
          [ui/GridColumn {:textAlign "center"}
           [ui/Segment {:style  {:background-color "#F9FAFB"}}
            [NetworkDataTimeSeries @selected-timespan (:network-stats @edge-stats)]
            [GraphLabel @selected-timespan]]]
          [ui/GridColumn {:textAlign "center"}
           [ui/Segment {:style  {:background-color "#F9FAFB"}}
            [RamUsageTimeSeries @selected-timespan (:ram-stats @edge-stats)]
            [GraphLabel @selected-timespan]]]]
         [ui/GridRow {:columns 1
                      :centered true}
          [ui/GridColumn {:textAlign "center"
                          :width 10}
           [ui/Segment {:style  {:background-color "#F9FAFB"}}
            [NEStatusTimeSeries @selected-timespan (:availability-stats @edge-stats)]
            [GraphLabel @selected-timespan]]]]]]])))
