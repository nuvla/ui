(ns sixsq.nuvla.ui.main.components
  (:require
    ["react" :as react]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.views :as history-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

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
  [_job-subs _set-active-tab-index-event _job-tab-index]
  (let [tr                (subscribe [::i18n-subs/tr])
        errors-dissmissed (r/atom #{})]
    (fn [job-subs set-active-tab-index-event job-tab-index]
      (let [all-jobs             (subscribe [job-subs])
            grouped-jobs         (group-by :action (-> @all-jobs :resources))
            contains-failed-jobs (into {}
                                       (map (fn [[action jobs]]
                                              (when (some #(when (= (:state %) "FAILED") jobs) jobs) {action jobs}))
                                            grouped-jobs))]
        [:<>
         (for [[action jobs] contains-failed-jobs]
           (let [last-job  (-> jobs first)
                 {:keys [state id]} last-job
                 last-line (last (str/split-lines (get last-job :status-message "")))]
             (when
               (and
                 (not (@errors-dissmissed id))
                 (= state "FAILED"))
               ^{:key action}
               [ui/Message {:error      true
                            :on-dismiss #(swap! errors-dissmissed conj id)}
                [ui/MessageHeader
                 {:style    {:cursor "pointer"}
                  :on-click #(dispatch [set-active-tab-index-event job-tab-index])}
                 (str (str/capitalize (@tr [:job])) " " action " " (@tr [:failed]))]
                [ui/MessageContent last-line]])))]))))


(defn BulkActionProgress
  [_opts]
  (let [open? (r/atom false)]
    (fn [{:keys [job on-dissmiss header]}]
      (let [{:keys [FAILED SUCCESS] :as status-message} (when (not= (:state job) "FAILED")
                                                          (general-utils/json->edn (:status-message job)))
            some-fail?    (pos? (count FAILED))
            some-success? (pos? (count SUCCESS))
            completed?    (= (:progress job) 100)
            state-failed? (= (:state job) "FAILED")
            color         (cond
                            (and some-fail? some-success?) "yellow"
                            (or state-failed? some-fail?) "red"
                            :else "green")
            progress-bar  (fn [label]
                            [ui/Progress (cond->
                                           {:active   (not completed?)
                                            :on-click #(reset! open? true)
                                            :percent  (:progress job)
                                            :progress true
                                            :color    color
                                            :size     "small"}
                                           label (assoc :label label
                                                        :style {:cursor "pointer"}))])]
        [ui/Message (when completed? {:on-dismiss on-dissmiss})
         [ui/MessageHeader header]
         [ui/MessageContent
          [:br]
          [ui/Modal {:trigger    (r/as-element (progress-bar "Click for more details"))
                     :close-icon true}
           [ui/ModalHeader header]
           [ui/ModalContent
            [:h3 "Progress:"]
            (progress-bar nil)
            (when state-failed?
              [:p status-message])
            (when (seq FAILED)
              [:<>
               [:h3 "Failed:"]
               [ui/ListSA
                (for [failed-id FAILED]
                  ^{:key failed-id}
                  [ui/ListItem
                   [ui/ListContent
                    [ui/ListHeader
                     {:as       :a
                      :href     failed-id
                      :target   "_blank"
                      :on-click (fn [event]
                                  (dispatch [::history-events/navigate failed-id])
                                  (.preventDefault event))} failed-id]
                    [ui/ListDescription (get-in status-message
                                                [:bootstrap-exceptions (keyword failed-id)])]]])]])
            (when (seq SUCCESS)
              [:<>
               [:h3 "Success:"]
               [ui/ListSA
                (for [success-id SUCCESS]
                  ^{:key success-id}
                  [ui/ListItem
                   [ui/ListContent
                    [ui/ListHeader [history-views/link success-id success-id]]]]
                  )]])
            ]]]]))))


(defn StatisticState
  ([value icons label clickable? set-state-selector-event state-selector-subs]
   (StatisticState value icons label clickable? "black" set-state-selector-event state-selector-subs))
  ([value icons label clickable? positive-color set-state-selector-event state-selector-subs]
   (let [state-selector (subscribe [state-selector-subs])
         selected?      (or
                          (= label @state-selector)
                          (and (= label "TOTAL")
                               (nil? @state-selector)))
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
                    :loading   (and (pos? value) (= "spinner" i))
                    :className i}])]]
      [ui/StatisticLabel label]])))


(defn ClickMeStaticPopup
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        is-small-device? (subscribe [::subs/is-small-device?])]
    (when-not @is-small-device?
      [ui/Popup
       {:trigger  (r/as-element [:span])
        :open     true
        :position "right center"
        :offset   [0 20]
        :style    {:z-index "auto"}                         ;to avoid pop up floating above modals
        }
       [ui/PopupContent
        [:span [ui/Icon {:name "arrow left"}] (@tr [:statistics-select-info])]]])))


(defn InfoPopup
  [message]
  [ui/Popup {:content message
             :trigger (r/as-element [ui/Icon {:name "info circle"}])}])


(defn NotFoundPortal
  [subs message-header message-content]
  (let [tr         (subscribe [::i18n-subs/tr])
        not-found? (subscribe [subs])]
    [ui/Dimmer {:active   @not-found?
                :inverted true}
     [ui/Segment {:textAlign "center"
                  :raised    true
                  :style     {:top    "20%"
                              :zIndex 1000}}
      [ui/Message {:warning true
                   :icon    "warning circle"
                   :header  (@tr [message-header])
                   :content (@tr [message-content])}]]]))
