(ns sixsq.nuvla.ui.utils.timeseries-components
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.datepicker :as datepicker-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.timeseries :as ts-utils]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn CustomPeriodSelector [timespan {:keys [on-change-fn-from on-change-fn-to]}]
  (let [tr                      (subscribe [::i18n-subs/tr])
        locale                  (subscribe [::i18n-subs/locale])]
    [:<>
     [datepicker-utils/datepickerWithLabel
      [datepicker-utils/label (@tr [:from])]
      [ui/DatePicker {:show-time-select false
                      :className        "datepicker"
                      :start-date       (:from timespan)
                      :end-date         (:to timespan)
                      :selected         (:from timespan)
                      :selects-start    true
                      :placeholderText  "Select a date"
                      :date-format      "dd/MM/yyyy"
                      :time-format      "HH:mm"
                      :max-date         (time/days-before 1)
                      :time-intervals   1
                      :locale           (or (time/locale-string->locale-object @locale) @locale)
                      :fixed-height     true
                      :on-change        on-change-fn-from}]]
     [datepicker-utils/datepickerWithLabel
      [datepicker-utils/label (str/capitalize (@tr [:to]))]
      [ui/DatePicker {:selects-end      true
                      :show-time-select false
                      :className        "datepicker"
                      :start-date       (:from timespan)
                      :end-date         (:to timespan)
                      :max-date         (time/now)
                      :min-date         (:from timespan)
                      :placeholderText  "Select a date"
                      :selected         (:to timespan)
                      :date-format      "dd/MM/yyyy"
                      :time-format      "HH:mm"
                      :time-intervals   1
                      :locale           (or (time/locale-string->locale-object @locale) @locale)
                      :fixed-height     true
                      :on-change        #(on-change-fn-to (if (time/is-today? %)
                                                            (time/now)
                                                            (time/end-of-day %)))}]]]))

(defn TimeSeriesDropdown [{:keys [loading? default-value timespan-options on-change-event]}]
  (r/with-let [currently-selected-option (r/atom default-value)
               custom-timespan           (r/atom {})]
    (let [tr (subscribe [::i18n-subs/tr])]
      [ui/MenuItem {:style {:padding-top    5
                            :padding-bottom 5
                            :padding-left   16
                            :height         45}}
       [:span {:style {:display      "flex"
                       :align-items  "center"
                       :margin-right 5}} (@tr [:showing-data-for])]
       [ui/Dropdown {:inline          true
                     :style           {:min-width       120
                                       :display         "flex"
                                       :justify-content "space-between"}
                     :loading         loading?
                     :close-on-change true
                     :default-value   default-value
                     :options         (mapv (fn [o] {:key o :text (@tr [(ts-utils/format-option o)]) :value o})
                                            timespan-options)
                     :on-change       (ui-callback/value
                                        (fn [timespan]
                                          (reset! currently-selected-option timespan)
                                          (when-not (= ts-utils/timespan-custom timespan)
                                            (let [[from to] (ts-utils/timespan-to-period timespan)]
                                              (reset! currently-selected-option timespan)
                                              (reset! custom-timespan {})
                                              (when on-change-event
                                                (dispatch [on-change-event
                                                           {:timespan-option timespan
                                                            :from            from
                                                            :to              to}]))))))}]
       [:div {:style {:display     "flex"
                      :margin-left 10
                      :visibility  (if (= ts-utils/timespan-custom @currently-selected-option)
                                     "visible"
                                     "hidden")}}
        [CustomPeriodSelector @custom-timespan
         {:on-change-fn-from #(do (swap! custom-timespan assoc :from %)
                                  (when (:to @custom-timespan)
                                    (when on-change-event
                                      (dispatch [on-change-event
                                                 {:from            %
                                                  :to              (:to @custom-timespan)
                                                  :timespan-option ts-utils/timespan-custom}]))))
          :on-change-fn-to   #(do (swap! custom-timespan assoc :to %)
                                  (when (:from @custom-timespan)
                                    (when on-change-event
                                      (dispatch [on-change-event
                                                 {:from            (:from @custom-timespan)
                                                  :to              %
                                                  :timespan-option ts-utils/timespan-custom}]))))}]]])))