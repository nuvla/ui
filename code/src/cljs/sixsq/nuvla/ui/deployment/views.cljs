(ns sixsq.nuvla.ui.deployment.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.deployment-detail.subs :as deployment-detail-subs]
    [sixsq.nuvla.ui.deployment-detail.views :as deployment-detail-views]
    [sixsq.nuvla.ui.deployment-dialog.views-module-version :as dep-diag-versions]
    [sixsq.nuvla.ui.deployment.events :as events]
    [sixsq.nuvla.ui.deployment.subs :as subs]
    [sixsq.nuvla.ui.deployment.utils :as utils]
    [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))


(defn refresh
  [& opts]
  (dispatch [::events/refresh opts]))


(defn control-bar []
  (let [full-text         (subscribe [::subs/full-text-search])
        additional-filter (subscribe [::subs/additional-filter])
        filter-open?      (r/atom false)]
    (fn []
      [ui/GridColumn
       [main-components/SearchInput
        {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))
         :default-value @full-text}]
       " "
       [ui/Popup
        {:content           "Additional filter"
         :mouse-enter-delay 500
         :on                "hover"
         :trigger           (r/as-element
                              ^{:key (random-uuid)}
                              [filter-comp/ButtonFilter
                               {:resource-name  "deployment"
                                :default-filter @additional-filter
                                :open?          filter-open?
                                :on-done        #(dispatch [::events/set-additional-filter %])}]
                              )}]])))


(defn BulkUpdateModal
  []
  (let [info            (subscribe [::subs/bulk-update-modal])
        versions        (subscribe [::deployment-detail-subs/module-versions])
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
         [uix/ModalHeader {:header "Bulk update"}]

         [ui/ModalContent
          [ui/Form
           (when-not module-href
             [ui/Message {:visible true
                          :warning true
                          :header  "Deployments not based on same module"
                          :content (str "Module selection is disabled because selected deployments "
                                        "are not based on the same module. You can still call this "
                                        "action to update container images if they are base on "
                                        "tags like latest.")}])
           [ui/FormDropdown
            {:scrolling   true
             :upward      false
             :selection   true
             :label       "Module version"
             :placeholder "Select a module version"
             :disabled    (nil? module-href)
             :on-change   (ui-callback/value
                            #(reset! selected-module
                                     (->> %
                                          (dep-diag-versions/get-version-id @versions)
                                          (str module-href "_"))))
             :fluid       true
             :options     options}]]]
         [ui/ModalActions
          [uix/Button {:text     "Launch"
                       :positive true
                       :active   true
                       :on-click #(dispatch [::events/bulk-update-operation @selected-module])}]]
         ]))))


(defn MenuBar
  []
  (let [view                  (subscribe [::subs/view])
        loading?              (subscribe [::subs/loading?])
        selected-set          (subscribe [::subs/selected-set])
        select-all?           (subscribe [::subs/select-all?])
        dep-count             (subscribe [::subs/deployments-count])
        selected-count        (subscribe [::subs/selected-count])
        is-all-page-selected? (subscribe [::subs/is-all-page-selected?])]
    (fn []
      [:<>
       [main-components/StickyBar
        [ui/Menu {:borderless true, :stackable true}
         [ui/MenuItem {:icon     "grid layout"
                       :active   (= @view "cards")
                       :on-click #(dispatch [::events/set-view "cards"])}]
         [ui/MenuItem {:icon     "table"
                       :active   (= @view "table")
                       :on-click #(dispatch [::events/set-view "table"])}]

         [ui/MenuItem {:on-click #(dispatch [::events/select-all])
                       :active   @select-all?}
          "Select all"]
         [ui/MenuItem {:active   @is-all-page-selected?
                       :on-click #(dispatch [::events/select-all-page])}
          "Select all in this page"]
         [ui/MenuItem {:disabled true}
          "Selected"
          [ui/Label
           (when (pos? @selected-count) {:color "teal"})
           (str @selected-count "/" @dep-count)]]
         [ui/MenuMenu
          [ui/Dropdown {:item     true :text "Actions"
                        :icon     "ellipsis vertical"
                        :disabled (not (pos? @selected-count))}
           [ui/DropdownMenu
            #_[ui/DropdownItem "Start"]
            #_[ui/DropdownItem "Stop"]
            [ui/DropdownItem
             {:on-click #(dispatch [::events/bulk-update @selected-set])}
             "Update"]]]]

         [main-components/RefreshMenu
          {:action-id  events/refresh-action-deployments-id
           :loading?   @loading?
           :on-refresh refresh}]]]
       [BulkUpdateModal]])))


(defn show-options
  [select-all? no-actions]
  (not (or select-all? (true? no-actions))))


(defn row-fn
  [{:keys [id state module parent nuvlabox] :as deployment}
   #_ {:clj-kondo/ignore [:unused-binding]}
   {:keys [no-actions no-module-name select-all] :as options}]
  (let [credential-id parent
        creds-name    (subscribe [::subs/creds-name-map])
        [primary-url-name
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
     [ui/TableCell [values/as-link (utils-general/id->uuid id)
                    :page "dashboard" :label (utils-general/id->short-uuid id)]]
     (when (not no-module-name)
       [ui/TableCell {:style {:overflow      "hidden",
                              :text-overflow "ellipsis",
                              :max-width     "20ch"}} (:name module)])
     [ui/TableCell state]
     [ui/TableCell (when url
                     [:a {:href url, :target "_blank", :rel "noreferrer"}
                      [ui/Icon {:name "external"}]
                      primary-url-name])]
     [ui/TableCell (-> deployment :created time/parse-iso8601 time/ago)]
     [ui/TableCell {:style {:overflow      "hidden",
                            :text-overflow "ellipsis",
                            :max-width     "20ch"}}
      (if nuvlabox
        (utils/format-nuvlabox-value nuvlabox)
        (get @creds-name credential-id credential-id))]
     (when show-options?
       [ui/TableCell
        (cond
          (utils-general/can-operation? "stop" deployment)
          [deployment-detail-views/ShutdownButton deployment]
          (utils-general/can-delete? deployment)
          [deployment-detail-views/DeleteButton deployment])])]))


(defn vertical-data-table
  #_ {:clj-kondo/ignore [:unused-binding]}
  [deployments-list options]
  (let [tr                    (subscribe [::i18n-subs/tr])
        is-all-page-selected? (subscribe [::subs/is-all-page-selected?])]
    (fn [deployments-list {:keys [no-actions no-module-name select-all] :as options}]
      (let [show-options? (show-options select-all no-actions)]
        (if (empty? deployments-list)
          [uix/WarningMsgNoElements]
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
             (when (not no-module-name)
               [ui/TableHeaderCell (@tr [:module])])
             [ui/TableHeaderCell (@tr [:status])]
             [ui/TableHeaderCell (@tr [:url])]
             [ui/TableHeaderCell (@tr [:created])]
             [ui/TableHeaderCell (@tr [:infrastructure])]
             (when show-options? [ui/TableHeaderCell (@tr [:actions])])]]
           [ui/TableBody
            (for [{:keys [id] :as deployment} deployments-list]
              ^{:key id}
              [row-fn deployment options])]])))))


(defn DeploymentCard
  [{:keys [id state module tags parent credential-name] :as deployment}]
  (let [tr            (subscribe [::i18n-subs/tr])
        {module-logo-url :logo-url
         module-name     :name
         module-content  :content} module
        [primary-url-name
         primary-url-pattern] (-> module-content (get :urls []) first)
        primary-url   (subscribe [::subs/deployment-url id primary-url-pattern])
        started?      (utils/is-started? state)
        dep-href      (utils/deployment-href id)
        select-all?   (subscribe [::subs/select-all?])
        is-selected?  (subscribe [::subs/is-selected? id])
        cred          (or credential-name parent)]
    ^{:key id}
    [uix/Card
     (cond-> {:header        [:span [:p {:style {:overflow      "hidden",
                                                 :text-overflow "ellipsis",
                                                 :max-width     "20ch"}} module-name]]
              :meta          (str (@tr [:created]) " " (-> deployment :created
                                                           time/parse-iso8601 time/ago))
              :description   (when cred
                               [:div [ui/Icon {:name "key"}] cred])
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
              :corner-button (cond
                               (utils-general/can-operation? "stop" deployment)
                               [deployment-detail-views/ShutdownButton deployment :label? true]

                               (utils-general/can-delete? deployment)
                               [deployment-detail-views/DeleteButton deployment :label? true])
              :state         state
              :loading?      (utils/deployment-in-transition? state)}

             (not @select-all?) (assoc :on-select #(dispatch [::events/select-id id])
                                       :selected? @is-selected?))]))


(defn cards-data-table
  [deployments-list]
  [:div style/center-items
   [ui/CardGroup {:centered    true
                  :itemsPerRow 4
                  :stackable   true}
    (for [{:keys [id] :as deployment} deployments-list]
      ^{:key id}
      [DeploymentCard deployment])]])


(defn deployments-display
  []
  (let [loading?    (subscribe [::subs/loading?])
        view        (subscribe [::subs/view])
        deployments (subscribe [::subs/deployments])
        select-all? (subscribe [::subs/select-all?])]
    (fn []
      (let [deployments-list (get @deployments :resources [])]
        [ui/Segment (merge style/basic
                           {:loading @loading?})
         (if (= @view "cards")
           [cards-data-table deployments-list]
           [vertical-data-table deployments-list {:select-all @select-all?}])]))))


(defn StatisticStates
  ([] [StatisticStates true])
  (#_ {:clj-kondo/ignore [:unused-binding]}
   [clickable?]
   (let [summary     (subscribe [::subs/deployments-summary])
         summary-all (subscribe [::subs/deployments-summary-all])]
     (fn [clickable?]
       (let [summary       (if clickable? summary summary-all)
             terms         (utils-general/aggregate-to-map
                             (get-in @summary [:aggregations :terms:state :buckets]))
             started       (:STARTED terms 0)
             starting      (:STARTING terms 0)
             created       (:CREATED terms 0)
             stopped       (:STOPPED terms 0)
             error         (:ERROR terms 0)
             pending       (:PENDING terms 0)
             starting-plus (+ starting created pending)
             total         (:count @summary)]
         [ui/GridColumn {:width      8
                         :text-align "center"}
          [ui/StatisticGroup (merge {:widths (if clickable? nil 5) :size "tiny"}
                                    {:style {:margin-right "0px"
                                             :display      "block"}})
           [main-components/StatisticState total ["fas fa-rocket"] "TOTAL" clickable?
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState started [(utils/status->icon utils/status-started)] utils/status-started
            clickable? "green"
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState starting-plus [(utils/status->icon utils/status-starting)]
            utils/status-starting clickable? "yellow"
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState stopped [(utils/status->icon utils/status-stopped)] utils/status-stopped
            clickable? "yellow"
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState error [(utils/status->icon utils/status-error)] utils/status-error
            clickable? "red" ::events/set-state-selector ::subs/state-selector]
           (when clickable?
             [main-components/ClickMeStaticPopup])]])))))


(defn DeploymentTable
  [options]
  (let [elements          (subscribe [::subs/deployments])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        loading?          (subscribe [::subs/loading?])
        select-all?       (subscribe [::subs/select-all?])]
    (fn []
      (let [total-elements (:count @elements)
            total-pages    (utils-general/total-pages total-elements @elements-per-page)
            deployments    (:resources @elements)]
        [ui/TabPane
         (if @loading?
           [ui/Loader {:active true
                       :inline "centered"}]
           [vertical-data-table deployments (assoc options :select-all @select-all?)])

         (when (pos? (:count @elements))
           [uix/Pagination {:totalitems   total-elements
                            :totalPages   total-pages
                            :activePage   @page
                            :onPageChange (ui-callback/callback
                                            :activePage
                                            #(dispatch [::events/set-page %]))}])]))))


(defn deployments-main-content
  []
  (let
    [elements-per-page   (subscribe [::subs/elements-per-page])
     page                (subscribe [::subs/page])
     dep-count           (subscribe [::subs/deployments-count])
     bulk-jobs-monitored (subscribe [::subs/bulk-jobs-monitored])]
    (refresh)
    (fn []
      (let [total-deployments @dep-count
            total-pages       (utils-general/total-pages
                                @dep-count @elements-per-page)]
        [:<>
         [MenuBar]
         [ui/Segment style/basic
          [ui/Grid {:columns   "equal"
                    :stackable true
                    :reversed  "mobile"}
           [control-bar]
           [StatisticStates true]
           [ui/GridColumn]]
          (for [[job-id job] @bulk-jobs-monitored]
            ^{:key job-id}
            [main-components/BulkActionProgress
             {:header      "Bulk update in progress"
              :job         job
              :on-dissmiss #(dispatch [::events/dissmiss-bulk-job-monitored job-id])}])
          [deployments-display]]
         [uix/Pagination
          {:totalitems   total-deployments
           :totalPages   total-pages
           :activePage   @page
           :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}]]))))
