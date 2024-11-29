(ns sixsq.nuvla.ui.common-components.plugins.duration-picker
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn decompose-duration
  [total-seconds]
  (let [days    (quot total-seconds 86400)
        hours   (quot (rem total-seconds 86400) 3600)
        minutes (quot (rem total-seconds 3600) 60)
        seconds (rem total-seconds 60)]
    {:days    days
     :hours   hours
     :minutes minutes
     :seconds seconds}))

(defn ->seconds
  [{:keys [days hours minutes seconds] :as value}]
  (apply + (remove nil?
                   [(some-> days (* 3600 24))
                    (some-> hours (* 3600))
                    (some-> minutes (* 60))
                    seconds])))

(defn DurationPicker
  [{:keys [::!value ::set-value-fn ::!disabled? ::tr-fn
           ::!show-days? ::!show-hours? ::!show-minutes? ::!show-seconds?
           ::!days-options ::!hours-options ::!minutes-options ::!seconds-options] :as _control}]
  (let [opts-fn   (fn [vals] (map (fn [i] {:key i :value i :content i :text i}) vals))
        {:keys [days hours minutes seconds] :as value} (decompose-duration @!value)
        set-value #(set-value-fn (->seconds %))]
    [:div {:style {:display               :grid
                   :grid-template-columns "repeat(auto-fill, minmax(100px, 1fr))"
                   :align-items           :center
                   :gap                   5}}
     (when @!show-days?
       [:div {:style {:display     :flex
                      :align-items :center
                      :gap         5}}
        [:label (tr-fn [:days])]
        [ui/Dropdown (cond-> {:class     :duration-days
                              :value     days
                              :options   (opts-fn @!days-options)
                              :selection true
                              :style     {:min-width "70px"}
                              :disabled  @!disabled?}
                             (not @!disabled?) (assoc :on-change (ui-callback/value #(set-value (assoc value :days %)))))]])
     (when @!show-hours?
       [:div {:style {:display     :flex
                      :align-items :center
                      :gap         5}}
        [:label (tr-fn [:hours])]
        [ui/Dropdown (cond-> {:class     :duration-hours
                              :value     hours
                              :options   (opts-fn @!hours-options)
                              :selection true
                              :style     {:min-width "70px"}
                              :disabled  @!disabled?}
                             (not @!disabled?) (assoc :on-change (ui-callback/value #(set-value (assoc value :hours %)))))]])
     (when @!show-minutes?
       [:div {:style {:display     :flex
                      :align-items :center
                      :gap         5}}
        [:label (tr-fn [:minutes])]
        [ui/Dropdown (cond-> {:class     :duration-minutes
                              :value     minutes
                              :options   (opts-fn @!minutes-options)
                              :selection true
                              :style     {:min-width "70px"}
                              :disabled  @!disabled?}
                             (not @!disabled?) (assoc :on-change (ui-callback/value #(set-value (assoc value :minutes %)))))]])
     (when @!show-seconds?
       [:div {:style {:display     :flex
                      :align-items :center
                      :gap         5}}
        [:label (tr-fn [:seconds])]
        [ui/Dropdown (cond-> {:class     :duration-seconds
                              :value     seconds
                              :options   (opts-fn @!seconds-options)
                              :selection true
                              :style     {:min-width "70px"}
                              :disabled  @!disabled?}
                             (not @!disabled?) (assoc :on-change (ui-callback/value #(set-value (assoc value :seconds %)))))]])]))

(defn DurationPickerController
  [{:keys [;; current value in seconds
           !value
           set-value-fn

           ;; Optional (all enabled by default)
           ;; whether to show days, hours, minutes and seconds
           !show-days?
           !show-hours?
           !show-minutes?
           !show-seconds?

           ;; Optional
           ;; Dropdown options to show
           !days-options
           !hours-options
           !minutes-options
           !seconds-options

           ;; Optional
           ;; whether the picker should be disabled
           !disabled?

           ;; Optional
           ;; Translations
           tr-fn
           ]}]
  (r/with-let [!value           (or !value (r/atom 0))
               set-value-fn     (or set-value-fn #(reset! !value %))
               !show-days?      (or !show-days? (r/atom true))
               !show-hours?     (or !show-hours? (r/atom true))
               !show-minutes?   (or !show-minutes? (r/atom true))
               !show-seconds?   (or !show-seconds? (r/atom true))
               !days-options    (or !days-options (r/atom (range 0 31)))
               !hours-options   (or !hours-options (r/atom (range 0 24)))
               !minutes-options (or !minutes-options (r/atom (range 0 60)))
               !seconds-options (or !seconds-options (r/atom (range 0 60)))
               !disabled?       (or !disabled? (r/atom false))
               tr-fn            (or tr-fn (comp str/capitalize name first))]
    [DurationPicker {::!value           !value
                     ::set-value-fn     set-value-fn
                     ::!show-days?      !show-days?
                     ::!show-hours?     !show-hours?
                     ::!show-minutes?   !show-minutes?
                     ::!show-seconds?   !show-seconds?
                     ::!days-options    !days-options
                     ::!hours-options   !hours-options
                     ::!minutes-options !minutes-options
                     ::!seconds-options !seconds-options
                     ::!disabled?       !disabled?
                     ::tr-fn            tr-fn}]))

