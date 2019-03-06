(ns sixsq.nuvla.ui.data.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.application.utils :as application-utils]
    [sixsq.nuvla.ui.data.events :as events]
    [sixsq.nuvla.ui.data.subs :as subs]
    [sixsq.nuvla.ui.data.utils :as utils]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as deployment-dialog-subs]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
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


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :on-click  #(dispatch [::events/get-data-sets])}])))


(defn process-button
  []
  (let [tr (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/selected-data-set-ids])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:process])
        :disabled  (not (seq @data-sets))
        :icon-name "rocket"
        :position  "left"
        :on-click  #(dispatch [::events/open-application-select-modal])}])))


(defn search-header []
  (let [tr (subscribe [::i18n-subs/tr])
        time-period (subscribe [::subs/time-period])
        locale (subscribe [::i18n-subs/locale])]
    (fn []
      (let [[time-start time-end] @time-period]
        [ui/Form
         [ui/FormGroup {:widths 3}
          [ui/FormField
           ;; FIXME: Find a better way to set the field width.
           [ui/DatePicker {:custom-input     (reagent/as-element [ui/Input {:label (@tr [:from])
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
           [ui/DatePicker {:custom-input     (reagent/as-element [ui/Input {:label (@tr [:to])
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
                           :on-change        #(dispatch [::events/set-time-period [time-start %]])}]]
          [ui/FormInput {:placeholder (@tr [:search])
                         :icon        "search"
                         :on-change   (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))}]]]))))


(defn control-bar []
  [:div
   [ui/Menu {:attached "top", :borderless true}
    [process-button]
    [ui/MenuMenu {:position "right"}
     [refresh-button]]]
   [ui/Segment {:attached "bottom"}
    [search-header]]])


(defn application-list-item
  [{:keys [id name description type created] :as application}]
  (let [selected-application-id (subscribe [::subs/selected-application-id])]
    (let [on-click-fn #(dispatch [::events/set-selected-application-id id])]
      ^{:key id}
      [ui/ListItem {:active   (and @selected-application-id (= id @selected-application-id))
                    :on-click on-click-fn}
       [ui/ListIcon {:name (application-utils/category-icon type), :size "large"}]
       [ui/ListContent
        [ui/ListHeader (str (or name id) " (" (time/ago (time/parse-iso8601 created)) ")")]
        (or description "")]])))


(defn application-list
  []
  (let [tr (subscribe [::i18n-subs/tr])
        applications (subscribe [::subs/applications])
        loading? (subscribe [::subs/loading-applications?])]
    [ui/Segment {:loading @loading?
                 :basic   true}
     (if (seq @applications)
       (vec (concat [ui/ListSA {:divided   true
                                :relaxed   true
                                :selection true}]
                    (mapv application-list-item @applications)))
       [ui/Message {:error true} (@tr [:no-apps])])]))


(defn launch-button
  []
  (let [tr (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/application-select-visible?])
        selected-application-id (subscribe [::subs/selected-application-id])

        data-step-active? (subscribe [::deployment-dialog-subs/data-step-active?])

        deployment (subscribe [::deployment-dialog-subs/deployment])
        data-completed? (subscribe [::deployment-dialog-subs/data-completed?])
        credentials-completed? (subscribe [::deployment-dialog-subs/credentials-completed?])
        size-completed? (subscribe [::deployment-dialog-subs/size-completed?])
        parameters-completed? (subscribe [::deployment-dialog-subs/parameters-completed?])]
    (fn []
      (let [hide-fn #(do
                       (dispatch [::events/close-application-select-modal])
                       (dispatch [::events/delete-deployment]))
            configure-fn (fn [id] (do
                                    (dispatch [::events/close-application-select-modal])
                                    (dispatch [::deployment-dialog-events/create-deployment id :data])))

            launch-fn (fn [id] (do
                                 (dispatch [::events/close-application-select-modal])
                                 (dispatch [::deployment-dialog-events/edit-deployment])))

            launch-disabled? (or (not @deployment)
                                 (and (not @data-completed?) @data-step-active?)
                                 (not @credentials-completed?)
                                 (not @size-completed?)
                                 (not @parameters-completed?))]

        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [ui/ModalHeader [ui/Icon {:name "sitemap"}] "\u00a0" (@tr [:select-application])]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription
           [application-list]]]
         [ui/ModalActions
          [ui/Button {:disabled (nil? @selected-application-id)
                      :primary  true
                      :on-click #(configure-fn @selected-application-id)}
           [ui/Icon {:name "settings"}]
           (@tr [:configure])]
          [ui/Button {:disabled (nil? @selected-application-id)
                      :primary  true
                      :on-click #()}
           [ui/Icon {:name     "rocket"
                     :disabled launch-disabled?
                     :on-click launch-fn}]
           (@tr [:launch])]]]))))


(defn application-select-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/application-select-visible?])
        selected-application-id (subscribe [::subs/selected-application-id])

        data-step-active? (subscribe [::deployment-dialog-subs/data-step-active?])

        deployment (subscribe [::deployment-dialog-subs/deployment])
        data-completed? (subscribe [::deployment-dialog-subs/data-completed?])
        credentials-completed? (subscribe [::deployment-dialog-subs/credentials-completed?])
        size-completed? (subscribe [::deployment-dialog-subs/size-completed?])
        parameters-completed? (subscribe [::deployment-dialog-subs/parameters-completed?])]
    (fn []
      (let [hide-fn #(do
                       (dispatch [::events/close-application-select-modal])
                       (dispatch [::events/delete-deployment]))
            configure-fn (fn [id] (do
                                    (dispatch [::events/close-application-select-modal])
                                    (dispatch [::deployment-dialog-events/create-deployment id :data])))

            launch-fn (fn [id] (do
                                 (dispatch [::events/close-application-select-modal])
                                 (dispatch [::deployment-dialog-events/edit-deployment])))

            launch-disabled? (or (not @deployment)
                                 (and (not @data-completed?) @data-step-active?)
                                 (not @credentials-completed?)
                                 (not @size-completed?)
                                 (not @parameters-completed?))]

        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [ui/ModalHeader [ui/Icon {:name "sitemap"}] "\u00a0" (@tr [:select-application])]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription
           [application-list]]]
         [ui/ModalActions
          [ui/Button {:disabled (nil? @selected-application-id)
                      :primary  true
                      :on-click #(configure-fn @selected-application-id)}
           [ui/Icon {:name "settings"}]
           (@tr [:configure])]
          [ui/Button {:disabled (or (nil? @selected-application-id)
                                    launch-disabled?)
                      :primary  true
                      :on-click launch-fn}
           [ui/Icon {:name "rocket"}]
           (@tr [:launch])]]]))))


(defn format-data-set-title
  [{:keys [id name] :as data-set}]
  (let [data-sets (subscribe [::subs/selected-data-set-ids])
        selected? (@data-sets id)]
    [ui/CardHeader {:style {:word-wrap "break-word"}}
     (or name id)
     (when selected? [ui/Label {:corner true
                                :icon   "pin"
                                :color  "blue"
                                :style  {:z-index 0}}])]))


(defn format-data-set
  [{:keys [id description] :as data-set}]
  (let [tr (subscribe [::i18n-subs/tr])
        counts (subscribe [::subs/counts])
        sizes (subscribe [::subs/sizes])
        count (get @counts id "...")
        size (get @sizes id "...")]
    ^{:key id}
    [ui/Card {:on-click #(dispatch [::events/toggle-data-set-id id])}
     [ui/CardContent
      [format-data-set-title data-set]
      [ui/CardDescription description]]
     [ui/CardContent {:extra true}
      [ui/Label
       [ui/Icon {:name "file"}]
       [:span (str count " " (@tr [:objects]))]]
      [ui/Label
       [ui/Icon {:name "expand arrows alternate"}]
       [:span (utils/format-bytes size)]]]]))


(defn queries-cards-group
  []
  (let [tr (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/data-sets])]
    [ui/Segment style/basic
     (if (seq @data-sets)
       [ui/Message {:info true}
        [ui/Icon {:name "pin"}]
        (@tr [:select-data-sets])]
       [ui/Message {:warning true}
        [ui/Icon {:name "warning sign"}]
        (@tr [:no-data-sets])])
     (when (seq @data-sets)
       (vec (concat [ui/CardGroup]
                    (map (fn [data-set]
                           [format-data-set data-set])
                         (vals @data-sets)))))]))


(defn data-set-resources
  []
  [ui/Container {:fluid true}
   [control-bar]
   [application-select-modal]
   [deployment-dialog-views/deploy-modal true]
   [queries-cards-group]])


(defmethod panel/render :data
  [path]

  ;; FIXME: find a better way to initialize credentials and data-sets
  (refresh-credentials)
  (refresh-data-sets)

  [data-set-resources])
