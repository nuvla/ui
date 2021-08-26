(ns sixsq.nuvla.ui.data.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.apps.utils :as application-utils]
    [sixsq.nuvla.ui.data.events :as events]
    [sixsq.nuvla.ui.data.subs :as subs]
    [sixsq.nuvla.ui.data.utils :as utils]
    [sixsq.nuvla.ui.data-record.views :as data-record]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as deployment-dialog-subs]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn refresh-credentials []
  (dispatch [::events/get-credentials]))


(defn refresh-data-sets []
  (dispatch [::events/get-data-sets]))


(defn refresh []
  (refresh-credentials)
  (refresh-data-sets))


(defn ProcessButton
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/selected-data-set-ids])]
    (fn []
      [uix/MenuItem
       {:name     (@tr [:process])
        :disabled (not (seq @data-sets))
        :icon     "rocket"
        :position "left"
        :on-click #(dispatch [::main-events/subscription-required-dispatch
                              [::events/open-application-select-modal]])}])))


(defn SearchHeader []
  (let [tr          (subscribe [::i18n-subs/tr])
        time-period (subscribe [::subs/time-period])
        locale      (subscribe [::i18n-subs/locale])]
    (fn []
      (let [[time-start time-end] @time-period]
        [ui/Form
         [ui/FormGroup {:widths 3}
          [ui/FormField
           ;; FIXME: Find a better way to set the field width.
           [ui/DatePicker {:custom-input     (r/as-element [ui/Input {:label (@tr [:from])
                                                                      :style {:min-width "25em"}}])
                           :selected         time-start
                           :start-date       time-start
                           :end-date         time-end
                           :max-date         time-end
                           :selects-start    true
                           :show-time-select true
                           :time-format      "HH:mm"
                           :time-intervals   15
                           :locale           @locale
                           :fixed-height     true
                           :date-format      "LLL"
                           :on-change        #(dispatch [::events/set-time-period [% time-end]])}]]
          ;; FIXME: Find a better way to set the field width.
          [ui/FormField
           [ui/DatePicker {:custom-input     (r/as-element [ui/Input {:label (@tr [:to])
                                                                      :style {:min-width "25em"}}])
                           :selected         time-end
                           :start-date       time-start
                           :end-date         time-end
                           :min-date         time-start
                           :max-date         (time/now)
                           :selects-end      true
                           :show-time-select true
                           :time-format      "HH:mm"
                           :time-intervals   15
                           :locale           @locale
                           :fixed-height     true
                           :date-format      "LLL"
                           :on-change        #(dispatch
                                                [::events/set-time-period [time-start %]])}]]
          [ui/FormField
           [main-components/SearchInput
            {:on-change (ui-callback/input-callback
                          #(dispatch [::events/set-full-text-search %]))}]]]]))))


(defn MenuBar []
  [:div
   [main-components/StickyBar
    [ui/Menu {:attached "top", :borderless true}
     [ProcessButton]
     [main-components/RefreshMenu
      {:on-refresh #(dispatch [::events/get-data-sets])}]]]
   [ui/Segment
    [SearchHeader]]])


(defn ApplicationListItem
  [{:keys [id name description subtype created] :as _application}]
  (let [selected-application-id (subscribe [::subs/selected-application-id])
        on-click-fn             #(dispatch [::events/set-selected-application-id id])]
    ^{:key id}
    [ui/ListItem {:active   (and @selected-application-id (= id @selected-application-id))
                  :on-click on-click-fn}
     [ui/ListIcon {:name (application-utils/subtype-icon subtype), :size "large"}]
     [ui/ListContent
      [ui/ListHeader (str (or name id) " (" (time/ago (time/parse-iso8601 created)) ")")]
      (or description "")]]))


(defn ApplicationList
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        applications (subscribe [::subs/applications])
        loading?     (subscribe [::subs/loading-applications?])]
    [ui/Segment {:loading @loading?
                 :basic   true}
     (if (seq @applications)
       (vec (concat [ui/ListSA {:divided   true
                                :relaxed   true
                                :selection true}]
                    (mapv ApplicationListItem @applications)))
       [ui/Message {:error true} (@tr [:no-apps])])]))


(defn LaunchButton
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        visible?               (subscribe [::subs/application-select-visible?])
        selected-app-id        (subscribe [::subs/selected-application-id])

        data-step-active?      (subscribe [::deployment-dialog-subs/data-step-active?])

        deployment             (subscribe [::deployment-dialog-subs/deployment])
        data-completed?        (subscribe [::deployment-dialog-subs/data-completed?])
        credentials-completed? (subscribe [::deployment-dialog-subs/credentials-completed?])
        env-completed?         (subscribe [::deployment-dialog-subs/env-variables-completed?])
        hide-fn                #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::events/delete-deployment]))
        configure-fn           (fn [id]
                                 (dispatch [::events/close-application-select-modal])
                                 (dispatch [::deployment-dialog-events/create-deployment id :data]))

        launch-fn              #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::deployment-dialog-events/edit-deployment]))]

    (fn []
      (let [launch-disabled? (or (not @deployment)
                                 (and (not @data-completed?) @data-step-active?)
                                 (not @credentials-completed?)
                                 (not @env-completed?))]

        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [uix/ModalHeader {:header (@tr [:select-application]) :icon "sitemap"}]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription
           [ApplicationList]]]
         [ui/ModalActions
          [ui/Button {:disabled (nil? @selected-app-id)
                      :primary  true
                      :on-click #(configure-fn @selected-app-id)}
           [ui/Icon {:name "settings"}]
           (@tr [:configure])]
          [ui/Button {:disabled (nil? @selected-app-id)
                      :primary  true
                      :on-click #()}
           [ui/Icon {:name     "rocket"
                     :disabled launch-disabled?
                     :on-click launch-fn}]
           (@tr [:launch])]]]))))


(defn ApplicationSelectModal
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        visible?               (subscribe [::subs/application-select-visible?])
        selected-app-id        (subscribe [::subs/selected-application-id])

        data-step-active?      (subscribe [::deployment-dialog-subs/data-step-active?])

        deployment             (subscribe [::deployment-dialog-subs/deployment])
        data-completed?        (subscribe [::deployment-dialog-subs/data-completed?])
        credentials-completed? (subscribe [::deployment-dialog-subs/credentials-completed?])
        env-completed?         (subscribe [::deployment-dialog-subs/env-variables-completed?])
        hide-fn                #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::events/delete-deployment]))
        configure-fn           (fn [id]
                                 (dispatch [::events/close-application-select-modal])
                                 (dispatch [::deployment-dialog-events/create-deployment id :data]))

        launch-fn              #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::deployment-dialog-events/edit-deployment]))]
    (fn []
      (let [launch-disabled? (or (not @deployment)
                                 (and (not @data-completed?) @data-step-active?)
                                 (not @credentials-completed?)
                                 (not @env-completed?))]

        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [uix/ModalHeader {:header (@tr [:select-application]) :icon "sitemap"}]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription
           [ApplicationList]]]
         [ui/ModalActions
          [ui/Button {:disabled (nil? @selected-app-id)
                      :primary  true
                      :on-click #(configure-fn @selected-app-id)}
           [ui/Icon {:name "settings"}]
           (@tr [:configure])]
          [ui/Button {:disabled (or (nil? @selected-app-id)
                                    launch-disabled?)
                      :primary  true
                      :on-click launch-fn}
           [ui/Icon {:name "rocket"}]
           (@tr [:launch])]]]))))


(defn DataSetCard
  [{:keys [id name description] :as _data-set}]
  (let [tr        (subscribe [::i18n-subs/tr])
        counts    (subscribe [::subs/counts])
        sizes     (subscribe [::subs/sizes])
        data-sets (subscribe [::subs/selected-data-set-ids])
        count     (get @counts id "...")
        size      (get @sizes id "...")
        selected? (boolean (@data-sets id))]
    ^{:key id}
    [uix/Card
     {:header      name
      :description description
      :extra       [:<>
                    [ui/Label
                     [ui/Icon {:name "file"}]
                     [:span (str count " " (@tr [:objects]))]]
                    [ui/Label
                     [ui/Icon {:name "expand arrows alternate"}]
                     [:span (utils/format-bytes size)]]]
      :on-select   #(dispatch [::events/toggle-data-set-id id])
      :selected?   selected?
      :on-click    (fn [event]
                     (dispatch [::history-events/navigate (utils/data-record-href id)])
                     (.preventDefault event))}]))


(defn QueriesCardsGroup
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/data-sets])]
    [ui/Segment style/basic
     (when (not (seq @data-sets))
       [ui/Message {:warning true}
        [ui/Icon {:name "warning sign"}]
        (@tr [:no-datasets])])
     (when (seq @data-sets)
       (vec (concat [ui/CardGroup {:centered true}]
                    (map (fn [data-set]
                           [DataSetCard data-set])
                         (vals @data-sets)))))]))


(defn MainActionButton
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/selected-data-set-ids])]
    [ui/ButtonGroup {:primary true}
     [ui/Button
      {:content  (@tr [:process])
       :disabled (not (seq @data-sets))
       :icon     "rocket"
       :on-click #(dispatch [::main-events/subscription-required-dispatch
                             [::events/open-application-select-modal]])}]]))


(defn DataSetResources
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment style/basic
     [uix/PageHeader "database" (@tr [:data-processing])]
     [MenuBar]
     [ApplicationSelectModal]
     [deployment-dialog-views/deploy-modal true]
     [QueriesCardsGroup]
     [MainActionButton]]))


(defmethod panel/render :data
  [path]
  (refresh)
  (let [[_ uuid] path
        n (count path)]
    (case n
      2 [data-record/DataRecords uuid]
      [DataSetResources])))
