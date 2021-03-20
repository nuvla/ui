(ns sixsq.nuvla.ui.main.components
  (:require
    ["react" :as react]
    [reagent.core :as r]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [clojure.string :as str]
    [reagent.core :as r]))

(def ref (react/createRef))


(defn RefreshedIn
  [action-id]
  (let [tr           (subscribe [::i18n-subs/tr])
        next-refresh (subscribe [::subs/next-refresh action-id])]
    (fn []
      [ui/MenuItem {:disabled true
                    :style    {:margin-right "10px"
                               :color        "grey"}}
       [:span (@tr [:automatic-refresh-in]) " "
        (when @next-refresh
          [uix/CountDown @next-refresh]) "s"]])))


(defn RefreshButton
  [loading? on-click refresh-disabled?]
  (let [tr       (subscribe [::i18n-subs/tr])
        on-click (or on-click #())]
    [uix/MenuItem
     {:name     (@tr [:refresh])
      :icon     "refresh"
      :loading? (boolean loading?)
      :on-click on-click
      :style    {:cursor "pointer"
                 :color  "black"}
      :disabled (boolean refresh-disabled?)}]))


(defn RefreshMenu
  [{:keys [action-id loading? on-refresh refresh-disabled?]}]
  [ui/MenuMenu {:position :right}
   (when action-id
     [RefreshedIn action-id])
   (when on-refresh
     [RefreshButton loading? on-refresh refresh-disabled?])])


(defn RefreshCompact
  [{:keys [action-id loading? on-refresh refresh-disabled?]}]
  [:span {:style {:display "inline-flex"}}
   (when action-id
     [RefreshedIn action-id])
   (when on-refresh
     [RefreshButton loading? on-refresh refresh-disabled?])])


(defn SearchInput
  [opts]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Input (merge {:placeholder (@tr [:search])
                      :icon        "search"}
                     opts)]))


(defn StickyBar [Menu]
  [ui/Sticky {:offset  -1
              :context ref
              :style   {:margin-top    11
                        :margin-bottom 10}}
   Menu])


(defn ErrorJobsMessage
  [resource-subs job-subs set-active-tab-index-event job-tab-index]
  (let [errors-dissmissed (r/atom #{})]
    (fn [resource-subscription]
      (let [{:keys [state]} @(subscribe [resource-subscription])
            jobs            (subscribe [job-subs])
            last-failed-job (some #(when (= (:state %) "FAILED") %) (:resources @jobs))
            action          (:action last-failed-job)
            id              (:id last-failed-job)
            last-line       (last (str/split-lines (get last-failed-job :status-message "")))]
        (when (and
                (not (@errors-dissmissed id))
                ;(= state "ERROR")
                (some? last-failed-job))
          [ui/Message {:error      true
                       :on-dismiss #(swap! errors-dissmissed conj id)}
           [ui/MessageHeader
            {:style    {:cursor "pointer"}
             :on-click #(dispatch [set-active-tab-index-event job-tab-index])}
            (str "Job " action " failed")]
           [ui/MessageContent last-line]])))))


(defn StatisticState
  ([value icons label clickable? set-state-selector-event state-selector-subs]
   (StatisticState value icons label clickable? "black" set-state-selector-event state-selector-subs))
  ([value icons label clickable? positive-color set-state-selector-event state-selector-subs]
   (let [state-selector (subscribe [state-selector-subs])
         selected?      (or
                          (= label @state-selector)
                          (and (= label "TOTAL")
                               (= @state-selector nil)))
         color          (if (pos? value) positive-color "grey")]
     [ui/Statistic {:style    (when clickable? {:cursor "pointer"})
                    :color    color
                    :class    (when clickable? "slight-up")
                    :on-click #(when clickable?
                                 (dispatch [set-state-selector-event
                                            (if (= label "TOTAL") nil label)]))}
      [ui/StatisticValue
       (or value "-")
       "\u2002"
       [ui/IconGroup
        (for [i icons]
          [ui/Icon {:key       (str "icon-" (str/join "-" i) "-id")
                    :size      (when (and clickable? selected?) "large")
                    :loading   (and (pos? value) (when (= "spinner" i)) true)
                    :className i}])]]
      [ui/StatisticLabel label]])))


(defn ClickMeStaticPopup
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Popup
     {:trigger  (r/as-element [:span])
      :open     true
      :position "right center"
      :offset   [0 20]
      :style    {:z-index "auto"}                           ;to avoid pop up floating above modals
      }
     [ui/PopupContent
      [:span [ui/Icon {:name "arrow left"}] (@tr [:statistics-select-info])]]]))


(defn InfoPopup
  [message]
  [ui/Popup {:content message
             :trigger (r/as-element [ui/Icon {:name "info circle"}])}])
