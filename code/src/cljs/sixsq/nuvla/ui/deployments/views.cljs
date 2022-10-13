(ns sixsq.nuvla.ui.deployments.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.deployment-dialog.views-module-version :as dep-diag-versions]
    [sixsq.nuvla.ui.deployments-detail.subs :as deployments-detail-subs]
    [sixsq.nuvla.ui.deployments-detail.views :as deployments-detail-views]
    [sixsq.nuvla.ui.deployments.events :as events]
    [sixsq.nuvla.ui.deployments.spec :as spec]
    [sixsq.nuvla.ui.deployments.subs :as subs]
    [sixsq.nuvla.ui.deployments.utils :as utils]
    [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
    [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))

(defn refresh
  []
  (dispatch [::events/refresh]))



(defn ControlBar []
  (let [additional-filter (subscribe [::subs/additional-filter])
        filter-open?      (r/atom false)]
    (fn []
      [ui/GridColumn {:width 4}
       [:div {:style {:display    :flex
                      :align-items :baseline}}
        [:div [full-text-search-plugin/FullTextSearch
               {:db-path      [::spec/deployments-search]
                :change-event [::pagination-plugin/change-page
                               [::spec/pagination] 1]
                :placeholder-suffix (str " " @(subscribe [::subs/state-selector]))}]]
        " "
        ^{:key (random-uuid)}
        [filter-comp/ButtonFilter
         {:resource-name  "deployment"
          :default-filter  @additional-filter
          :open?          filter-open?
          :on-done        #(dispatch [::events/set-additional-filter %])}]]])))

(defn BulkUpdateModal
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        info            (subscribe [::subs/bulk-update-modal])
        versions        (subscribe [::deployments-detail-subs/module-versions])
        selected-module (r/atom nil)]
    (fn []
      (let [options     (map (fn [[idx {:keys [href commit]}]]
                               {:key   idx,
                                :value href
                                :text  (str "v" idx " | " commit)}) @versions)
            module-href (:module-href @info)]
        [ui/Modal {:open       (some? @info)
                   :close-icon true
                   :on-close   #(dispatch [::events/close-modal-bulk-update])}
         [uix/ModalHeader {:header (@tr [:bulk-deployment-update])}]

         [ui/ModalContent
          [ui/Form
           (when-not module-href
             [ui/Message {:visible true
                          :warning true
                          :header  (@tr [:deployment-based-different-module])
                          :content (@tr [:deployment-based-different-module-details])}])
           [ui/FormDropdown
            {:scrolling   true
             :upward      false
             :selection   true
             :label       (@tr [:module-version])
             :placeholder (@tr [:select-version])
             :disabled    (nil? module-href)
             :on-change   (ui-callback/value
                            #(reset! selected-module
                                     (->> %
                                          (dep-diag-versions/get-version-id @versions)
                                          (str module-href "_"))))
             :fluid       true
             :options     options}]]]
         [ui/ModalActions
          [uix/Button {:text     (str/capitalize (@tr [:bulk-deployment-update]))
                       :positive true
                       :active   true
                       :on-click #(dispatch [::events/bulk-operation
                                             "bulk-update"
                                             {:module-href @selected-module}
                                             [::events/close-modal-bulk-update]])}]]]))))

(defn MenuBar
  []
  (let [tr                    (subscribe [::i18n-subs/tr])
        view                  (subscribe [::subs/view])
        select-all?           (subscribe [::subs/select-all?])
        dep-count             (subscribe [::subs/deployments-count])
        selected-count        (subscribe [::subs/selected-count])
        is-all-page-selected? (subscribe [::subs/is-all-page-selected?])
        modal-stop-key        (r/atom (random-uuid))
        modal-bulk-delete-key (r/atom (random-uuid))]
    (fn []
      [:<>
       [components/StickyBar
        [ui/Menu {:borderless true, :stackable true}
         [ui/MenuItem {:icon     "grid layout"
                       :active   (= @view "cards")
                       :on-click #(dispatch [::events/set-view "cards"])}]
         [ui/MenuItem {:icon     "table"
                       :active   (= @view "table")
                       :on-click #(dispatch [::events/set-view "table"])}]

         [ui/MenuItem {:on-click #(dispatch [::events/select-all])
                       :active   @select-all?}
          (@tr [:select-all])]
         [ui/MenuItem {:active   @is-all-page-selected?
                       :on-click #(dispatch [::events/select-all-page])}
          (@tr [:select-all-page])]
         [ui/MenuItem {:disabled true}
          (@tr [:selected])
          [ui/Label
           (when (pos? @selected-count) {:color "teal"})
           (str @selected-count "/" @dep-count)]]
         [ui/MenuMenu
          [ui/Dropdown {:item     true :text (@tr [:bulk-action])
                        :icon     "ellipsis vertical"
                        :disabled (not (pos? @selected-count))}
           [ui/DropdownMenu
            #_[ui/DropdownItem "Start"]
            [ui/DropdownItem
             {:on-click #(dispatch [::events/bulk-update-params])} (str/capitalize (@tr [:update]))]
            ^{:key @modal-stop-key}
            [uix/ModalDanger
             {:on-confirm  #(do
                              (dispatch [::events/bulk-operation "bulk-stop"])
                              (swap! modal-stop-key random-uuid))
              :trigger     (r/as-element [ui/DropdownItem (str/capitalize (@tr [:stop]))])
              :header      (@tr [:bulk-deployment-stop])
              :danger-msg  (@tr [:danger-action-cannot-be-undone])
              :button-text (str/capitalize (@tr [:bulk-deployment-stop]))}]
            ^{:key @modal-bulk-delete-key}
            [uix/ModalDanger
             {:on-confirm  #(do
                              (dispatch [::events/bulk-operation "bulk-force-delete"])
                              (swap! modal-bulk-delete-key random-uuid))
              :trigger     (r/as-element [ui/DropdownItem (str/capitalize (@tr [:force-delete]))])
              :header      (@tr [:bulk-deployment-force-delete])
              :danger-msg  (@tr [:danger-action-deployment-force-delete])
              :button-text (str/capitalize (@tr [:bulk-deployment-force-delete]))}]]]]

         [components/RefreshMenu
          {:action-id  events/refresh-action-deployments-id
           :on-refresh refresh}]]]
       [BulkUpdateModal]])))

(defn show-options
  [select-all? no-actions]
  (not (or select-all? (true? no-actions))))

(defn RowFn
  [{:keys [id state module] :as deployment}
   {:keys [no-actions no-module-name select-all] :as _options}]
  (let [[primary-url-name
         primary-url-pattern] (-> module :content (get :urls []) first)
        url           @(subscribe [::subs/deployment-url id primary-url-pattern])
        selected?     (subscribe [::subs/is-selected? id])
        show-options? (show-options select-all no-actions)]
    [ui/TableRow
     (when show-options?
       [ui/TableCell
        [ui/Checkbox {:checked  @selected?
                      :on-click (fn [event]
                                  (dispatch [::events/select-id id])
                                  (.stopPropagation event))}]])
     [ui/TableCell [values/as-link (general-utils/id->uuid id)
                    :page "deployment" :label (general-utils/id->short-uuid id)]]
     (when-not no-module-name
       [ui/TableCell {:style {:overflow      "hidden",
                              :text-overflow "ellipsis",
                              :max-width     "20ch"}} (:name module)])
     [ui/TableCell (utils/deployment-version deployment)]
     [ui/TableCell state]
     [ui/TableCell (when url
                     [:a {:href url, :target "_blank", :rel "noreferrer"}
                      [ui/Icon {:name "external"}]
                      primary-url-name])]
     [ui/TableCell (-> deployment :created time/parse-iso8601 time/ago)]
     [ui/TableCell {:style {:overflow      "hidden",
                            :text-overflow "ellipsis",
                            :max-width     "20ch"}}
      [utils/CloudNuvlaEdgeLink deployment]]
     (when show-options?
       [ui/TableCell
        (cond
          (general-utils/can-operation? "stop" deployment)
          [deployments-detail-views/ShutdownButton deployment]
          (general-utils/can-delete? deployment)
          [deployments-detail-views/DeleteButton deployment])])]))

(defn VerticalDataTable
  [_deployments-list _options]
  (let [tr                    (subscribe [::i18n-subs/tr])
        is-all-page-selected? (subscribe [::subs/is-all-page-selected?])]
    (fn [deployments-list {:keys [no-actions no-module-name select-all empty-msg] :as options}]
      (let [show-options? (show-options select-all no-actions)]
        (if (empty? deployments-list)
          [uix/WarningMsgNoElements empty-msg]
          [ui/Table
           (merge style/single-line {:stackable true})
           [ui/TableHeader
            [ui/TableRow
             (when show-options?
               [ui/TableHeaderCell
                [ui/Checkbox
                 {:checked  @is-all-page-selected?
                  :on-click #(dispatch [::events/select-all-page])}]])
             [ui/TableHeaderCell (@tr [:id])]
             (when-not no-module-name
               [ui/TableHeaderCell (@tr [:module])])
             [ui/TableHeaderCell (@tr [:version])]
             [ui/TableHeaderCell (@tr [:status])]
             [ui/TableHeaderCell (@tr [:url])]
             [ui/TableHeaderCell (@tr [:created])]
             [ui/TableHeaderCell (@tr [:infrastructure])]
             (when show-options? [ui/TableHeaderCell (@tr [:actions])])]]
           [ui/TableBody
            (for [{:keys [id] :as deployment} deployments-list]
              ^{:key id}
              [RowFn deployment options])]])))))

(defn DeploymentCard
  [{:keys [id state module tags] :as deployment}]
  (let [tr           (subscribe [::i18n-subs/tr])
        {module-logo-url :logo-url
         module-name     :name
         module-content  :content} module
        [primary-url-name
         primary-url-pattern] (-> module-content (get :urls []) first)
        primary-url  (subscribe [::subs/deployment-url id primary-url-pattern])
        started?     (utils/started? state)
        dep-href     (utils/deployment-href id)
        select-all?  (subscribe [::subs/select-all?])
        is-selected? (subscribe [::subs/is-selected? id])]
    ^{:key id}
    [uix/Card
     (cond-> {:header        [:span [:p {:style {:overflow      "hidden",
                                                 :text-overflow "ellipsis",
                                                 :max-width     "20ch"}} module-name]]
              :meta          (str (@tr [:created]) " " (-> deployment :created
                                                           time/parse-iso8601 time/ago))
              :description   [utils/CloudNuvlaEdgeLink deployment :link false]
              :tags          tags
              :button        (when (and started? @primary-url)
                               [ui/Button {:color    "green"
                                           :icon     "external"
                                           :content  primary-url-name
                                           :fluid    true
                                           :on-click (fn [event]
                                                       (dispatch [::main-events/open-link
                                                                  @primary-url])
                                                       (.preventDefault event)
                                                       (.stopPropagation event))
                                           :target   "_blank"
                                           :rel      "noreferrer"}])
              :on-click      (fn [event]
                               (dispatch [::history-events/navigate (utils/deployment-href id)])
                               (.preventDefault event))
              :href          dep-href
              :image         (or module-logo-url "")
              :left-state    (utils/deployment-version deployment)
              :corner-button (cond
                               (general-utils/can-operation? "stop" deployment)
                               [deployments-detail-views/ShutdownButton deployment :label? true]

                               (general-utils/can-delete? deployment)
                               [deployments-detail-views/DeleteButton deployment :label? true])
              :state         state}

             (not @select-all?) (assoc :on-select #(dispatch [::events/select-id id])
                                       :selected? @is-selected?))]))

(defn CardsDataTable
  [deployments-list]
  [:div style/center-items
   [ui/CardGroup {:centered    true
                  :itemsPerRow 4
                  :stackable   true}
    (for [{:keys [id] :as deployment} deployments-list]
      ^{:key id}
      [DeploymentCard deployment])]])

(defn DeploymentsDisplay
  []
  (let [view        (subscribe [::subs/view])
        deployments (subscribe [::subs/deployments])
        select-all? (subscribe [::subs/select-all?])]
    (fn []
      (let [deployments-list (get @deployments :resources [])]
        [ui/Segment {:basic true}
         (if (= @view "cards")
           [CardsDataTable deployments-list]
           [VerticalDataTable deployments-list {:select-all @select-all?}])]))))

(defn StatisticStates
  [_clickable? summary-subs]
  (let [summary (subscribe [summary-subs])]
    (fn [clickable? _summary-subs]
      (let [terms         (general-utils/aggregate-to-map
                            (get-in @summary [:aggregations :terms:state :buckets]))
            started       (:STARTED terms 0)
            starting      (:STARTING terms 0)
            created       (:CREATED terms 0)
            stopped       (:STOPPED terms 0)
            error         (:ERROR terms 0)
            pending       (:PENDING terms 0)
            starting-plus (+ starting created pending)
            total         (:count @summary)]
        [ui/GridColumn {:width 8}
         [ui/StatisticGroup {:size  "tiny"
                             :style {:justify-content "center"}}
          [components/StatisticState total ["fas fa-rocket"] "TOTAL" clickable?
           ::events/set-state-selector ::subs/state-selector]
          [components/StatisticState started [(utils/state->icon utils/STARTED)] utils/STARTED
           clickable? "green"
           ::events/set-state-selector ::subs/state-selector]
          [components/StatisticState starting-plus [(utils/state->icon utils/STARTING)]
           utils/STARTING clickable? "yellow"
           ::events/set-state-selector ::subs/state-selector]
          [components/StatisticState stopped [(utils/state->icon utils/STOPPED)] utils/STOPPED
           clickable? "yellow"
           ::events/set-state-selector ::subs/state-selector]
          [components/StatisticState error [(utils/state->icon utils/ERROR)] utils/ERROR
           clickable? "red" ::events/set-state-selector ::subs/state-selector]]]))))

(defn DeploymentsOverviewSegment
  [deployment-subs set-active-tab-event deployment-tab-key on-click]
  (let [tr    (subscribe [::i18n-subs/tr])
        icon  "rocket"
        color "blue"]
    [ui/Segment {:secondary true
                 :color     color
                 :raised    true
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"}}

     [:h4 [ui/Icon {:name icon}] (str/capitalize (@tr [:deployments]))]

     [StatisticStates false deployment-subs]

     [ui/Button {:color    color
                 :icon     icon
                 :style    {:align-self "start"}
                 :content  "Show me"
                 :on-click (or on-click
                               #(dispatch [set-active-tab-event deployment-tab-key]))}]]))

(defn Pagination
  []
  (let [dep-count @(subscribe [::subs/deployments-count])]
    [pagination-plugin/Pagination
     {:db-path      [::spec/pagination]
      :total-items  dep-count
      :change-event [::events/refresh]}]))

(defn DeploymentTable
  [options]
  (let [elements    (subscribe [::subs/deployments])
        select-all? (subscribe [::subs/select-all?])]
    (fn []
      (let [deployments (:resources @elements)]
        [:<>
         [VerticalDataTable
          deployments (assoc options :select-all @select-all?)]
         [Pagination]]))))

(defn DeploymentsMainContent
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        bulk-jobs-monitored (subscribe [::subs/bulk-jobs-monitored])]
    (dispatch [::events/init])
    (fn []
      [components/LoadingPage {}
       [:<>
        [uix/PageHeader "rocket"
         (general-utils/capitalize-first-letter (@tr [:deployments]))]
        [MenuBar]
        [ui/Grid {:stackable true
                  :reversed  "mobile"}
         [ControlBar]
         [StatisticStates true ::subs/deployments-summary]]
        (for [[job-id job] @bulk-jobs-monitored]
          ^{:key job-id}
          [components/BulkActionProgress
           {:header      "Bulk update in progress"
            :job         job
            :on-dissmiss #(dispatch [::events/dissmiss-bulk-job-monitored job-id])}])
        [DeploymentsDisplay]
        [Pagination]]])))

(defmethod panel/render :deployments
  [path]
  (let [[_ uuid] path
        n        (count path)
        children (case n
                   2 [deployments-detail-views/DeploymentDetails uuid]
                   [DeploymentsMainContent])]
    [ui/Segment style/basic children]))
