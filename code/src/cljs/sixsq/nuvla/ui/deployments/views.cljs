(ns sixsq.nuvla.ui.deployments.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.about.subs :as about-subs]
            [sixsq.nuvla.ui.about.utils :as about-utils]
            [sixsq.nuvla.ui.deployments-detail.subs :as deployments-detail-subs]
            [sixsq.nuvla.ui.deployments-detail.views :as deployments-detail-views]
            [sixsq.nuvla.ui.deployments.events :as events]
            [sixsq.nuvla.ui.deployments.spec :as spec]
            [sixsq.nuvla.ui.deployments.subs :as subs]
            [sixsq.nuvla.ui.deployments.utils :as utils :refer [build-bulk-filter]]
            [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.module :refer [get-version-id]]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [Table]]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.bulk-edit-tags-modal :as bulk-edit-modal]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.view-components :as vc :refer [TitledCard]]))

(def deployments-resources-subs-key [::subs/deployments-resources])

(defn refresh
  []
  (dispatch [::events/refresh]))

(defn ControlBar []
  (let [additional-filter (subscribe [::subs/additional-filter])
        filter-open?      (r/atom false)]
    (fn []
      [ui/GridColumn {:width 4}
       [:div
        [:div [full-text-search-plugin/FullTextSearch
               {:db-path            [::spec/deployments-search]
                :change-event       [::pagination-plugin/change-page
                                     [::spec/pagination] 1]
                :placeholder-suffix (str " " @(subscribe [::subs/state-selector]))}]]
        " "
        ^{:key (random-uuid)}
        [:div {:style {:margin-top "10px"}}
         [filter-comp/ButtonFilter
          {:resource-name                    spec/resource-name
           :default-filter                    @additional-filter
           :open?                            filter-open?
           :on-done                          #(dispatch [::events/set-additional-filter %])
           :show-clear-button-outside-modal? true}]]]])))

(defn BulkUpdateModal
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        info            (subscribe [::subs/bulk-update-modal])
        versions        (subscribe [::deployments-detail-subs/module-versions])
        version-options (subscribe [::deployments-detail-subs/module-versions-options])
        selected-module (r/atom nil)]
    (fn []
      (let [module-href (:module-href @info)]
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
                                          (get-version-id @versions)
                                          (str module-href "_"))))
             :fluid       true
             :options     @version-options}]]]
         [ui/ModalActions
          [uix/Button {:text     (str/capitalize (@tr [:bulk-deployment-update]))
                       :positive true
                       :active   true
                       :on-click #(dispatch [::events/bulk-operation
                                             "bulk-update"
                                             {:module-href @selected-module}
                                             [::events/close-modal-bulk-update]])}]]]))))

(defn BulkActionModal
  [{:keys [on-confirm trigger open? header danger-msg button-text close-event]}]
  [uix/ModalDanger
   {:on-confirm  (fn [] (on-confirm))
    :open        open?
    :on-close    (fn [] (dispatch close-event))
    :trigger     (r/as-element
                   [:div {:on-click
                          (fn [] (when open?
                                   (dispatch close-event)))} trigger])
    :header      header
    :danger-msg  danger-msg
    :button-text button-text}])

(defn BulkStopModal
  []
  (let [tr    (subscribe [::i18n-subs/tr])
        open? (subscribe [::subs/bulk-stop-modal])]
    (fn []
      [BulkActionModal
       {:open?       @open?
        :close-event [::events/close-modal-bulk-stop]
        :on-confirm  (fn []
                       (dispatch [::events/bulk-operation "bulk-stop" nil [::events/close-modal-bulk-stop]]))
        :header      (@tr [:bulk-deployment-stop])
        :danger-msg  (@tr [:danger-action-cannot-be-undone])
        :button-text (str/capitalize (@tr [:bulk-deployment-stop]))}])))

(defn BulkForceDeleteModal
  []
  (let [tr    (subscribe [::i18n-subs/tr])
        open? (subscribe [::subs/bulk-delete-modal])]
    (fn []
      [BulkActionModal
       {:open?       @open?
        :close-event [::events/close-modal-bulk-delete]
        :on-confirm  (fn [] (dispatch [::events/bulk-operation "bulk-force-delete"
                                       nil
                                       [::events/close-modal-bulk-delete]]))
        :header      (@tr [:bulk-deployment-force-delete])
        :danger-msg  (@tr [:danger-action-deployment-force-delete])
        :button-text (str/capitalize (@tr [:bulk-deployment-force-delete]))}])))

(defn MenuBar
  []
  (let [view (subscribe [::subs/view])]
    (fn []
      [:<>
       [components/StickyBar
        [ui/Menu {:borderless true, :stackable true}
         [ui/MenuItem {:icon     icons/i-grid-layout
                       :active   (= @view "cards")
                       :on-click #(dispatch [::events/set-view "cards"])}]
         [ui/MenuItem {:icon     "table"
                       :active   (= @view "table")
                       :on-click #(dispatch [::events/set-view "table"])}]

         [components/RefreshMenu
          {:action-id  events/refresh-action-deployments-id
           :on-refresh refresh}]]]])))

(defn- DeplSetLink
  [depl-set-id]
  (when depl-set-id
    (let [name (subscribe [::subs/deployment-set-name depl-set-id])]
      [:a {:href depl-set-id}
       [ui/Icon {:name "bullseye"}]
       @name])))


(defn RowFn
  [{:keys [id state module tags created-by] :as deployment}
   {:keys [no-module-name show-options?] :as options}]
  (let [[primary-url-name
         primary-url-pattern] (-> module :content (get :urls []) first)
        url                   @(subscribe [::subs/deployment-url id primary-url-pattern])
        creator               (subscribe [::session-subs/resolve-user created-by])
        edge-id               (:nuvlabox deployment)
        edge-status            (subscribe [::subs/deployment-edges-stati edge-id])]
    [:<>
     [ui/TableCell [:a {:href (name->href routes/deployment-details {:uuid (general-utils/id->uuid id)})}
                    (general-utils/id->short-uuid id)]]
     (when-not no-module-name
       [ui/TableCell {:style {:overflow      "hidden",
                              :text-overflow "ellipsis",
                              :max-width     "20ch"}}
        [:div {:class "app-icon-name"
               :style {:display     :flex
                       :align-items :center}}
         [:img {:src   (or (:thumb-nail module) (:logo-url module))
                :style {:width  "42px"
                        :height "30px"}}]
         [:div (:name module)]]])
     [ui/TableCell (utils/deployment-version deployment)]
     [ui/TableCell state]
     [ui/TableCell (when url
                     [:a {:href url, :target "_blank", :rel "noreferrer"}
                      [ui/Icon {:name "external"}]
                      primary-url-name])]
     (when (:show-depl-set-column? options)
       [ui/TableCell [DeplSetLink (deployment :deployment-set)]])
     [ui/TableCell (-> deployment :created time/parse-iso8601 time/ago)]
     [ui/TableCell (-> deployment :updated time/parse-iso8601 time/ago)]
     [ui/TableCell @creator]
     [ui/TableCell [uix/Tags tags]]
     [ui/TableCell {:style {:overflow      "hidden",
                            :text-overflow "ellipsis",
                            :max-width     "20ch"}}
      [utils/CloudNuvlaEdgeLink deployment
       :color (when edge-id (vc/status->color @edge-status))]]
     (when show-options?
       [ui/TableCell
        (cond
          (general-utils/can-operation? "stop" deployment)
          [deployments-detail-views/ShutdownButton deployment]
          (general-utils/can-delete? deployment)
          [deployments-detail-views/DeleteButton deployment])])]))


(defn VerticalDataTable
  [_deployments-list {:keys [hide-depl-group-column?] :as _options}]
  (let [tr (subscribe [::i18n-subs/tr])
        ff (subscribe [::about-subs/feature-flag-enabled? about-utils/feature-deployment-set-key])
        show-depl-set-column? (and @ff (not hide-depl-group-column?))]
    (fn [deployments-list {:keys [show-options? no-module-name empty-msg] :as options}]
      (dispatch [::events/get-deployment-groups-names deployments-list])
      (if (empty? deployments-list)
        [uix/WarningMsgNoElements empty-msg]
        (let [selectable? (or (nil? show-options?) show-options?)
              {trigger           :trigger-config
               BulkEditTagsModal :modal} (bulk-edit-modal/create-bulk-edit-modal
                                           {:db-path                [::spec/select]
                                            :refetch-event          ::events/get-deployments
                                            :resource-key           :deployment
                                            :total-count-sub-key    ::subs/deployments-count
                                            :on-open-modal-event    ::events/get-deployments-without-edit-rights
                                            :no-edit-rights-sub-key ::subs/deployments-without-edit-rights
                                            :singular               (@tr [:deployment])
                                            :plural                 (@tr [:deployments])
                                            :filter-fn              build-bulk-filter})]
          [:<>
           [BulkEditTagsModal]
           [Table {:columns       [{:field-key :id}
                                   (when-not no-module-name
                                     {:field-key      :module.name
                                      :header-content (@tr [:module])})
                                   {:field-key :version :no-sort? true}
                                   {:field-key :status
                                    :sort-key  :state}
                                   {:field-key :url
                                    :no-sort?  true}
                                   (when show-depl-set-column?
                                     {:field-key :deployment-set
                                      :no-sort?  true})
                                   {:field-key :created}
                                   {:field-key :updated}
                                   {:field-key :created-by}
                                   {:field-key :tags}
                                   {:field-key :infrastructure
                                    :no-sort?  true}
                                   (when selectable? {:field-key :actions
                                                      :no-sort?  true})]
                   :rows          deployments-list
                   :sort-config   {:db-path     ::spec/ordering
                                   :fetch-event (or (:fetch-event options) [::events/get-deployments])}
                   :row-render    (fn [deployment] [RowFn deployment (assoc options
                                                                       :show-options? selectable?
                                                                       :show-depl-set-column? show-depl-set-column?)])
                   :table-props   (merge style/single-line {:stackable true})
                   :select-config (when selectable?
                                    {:bulk-actions        [{:event [::events/bulk-update-params]
                                                            :name  (str/capitalize (@tr [:update]))}
                                                           {:event [::events/open-modal-bulk-stop]
                                                            :name  (str/capitalize (@tr [:stop]))}
                                                           {:event [::events/open-modal-bulk-delete]
                                                            :name  (str/capitalize (@tr [:delete]))}
                                                           trigger]
                                     :select-db-path      [::spec/select]
                                     :total-count-sub-key [::subs/deployments-count]
                                     :resources-sub-key   deployments-resources-subs-key
                                     :rights-needed       :edit})}]])))))

(defn DeploymentCard
  [{:keys [id state module tags created-by] :as deployment}]
  (let [tr          (subscribe [::i18n-subs/tr])
        {module-logo-url :logo-url
         module-name     :name
         module-content  :content} module
        [primary-url-name
         primary-url-pattern] (-> module-content (get :urls []) first)
        primary-url (subscribe [::subs/deployment-url id primary-url-pattern])
        started?    (utils/started? state)
        creator     (subscribe [::session-subs/resolve-user created-by])]
    ^{:key id}
    [uix/Card
     (cond-> {:header        [:span [:p {:style {:overflow      "hidden",
                                                 :text-overflow "ellipsis",
                                                 :max-width     "20ch"}} module-name]]
              :meta          [:<>
                              [:div (str (@tr [:created]) " " (-> deployment :created
                                                                  time/parse-iso8601 time/ago))]
                              (when @creator [:div (str (@tr [:by]) " " @creator)])]
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
                               (dispatch [::routing-events/navigate (utils/deployment-href id)])
                               (.preventDefault event))
              :image         (or module-logo-url "")
              :left-state    (utils/deployment-version deployment)
              :corner-button (cond
                               (general-utils/can-operation? "stop" deployment)
                               [deployments-detail-views/ShutdownButton deployment :label? true]

                               (general-utils/can-delete? deployment)
                               [deployments-detail-views/DeleteButton deployment :label? true])
              :state         state})]))

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
        deployments (subscribe deployments-resources-subs-key)]
    (fn []
      [:div
       [BulkUpdateModal]
       [BulkStopModal]
       [BulkForceDeleteModal]
       (if (= @view "cards")
         [CardsDataTable @deployments]
         [VerticalDataTable @deployments])])))

(defn state-aggs->state->count
  [summary]
  (let [terms         (general-utils/aggregate-to-map
                        (get-in summary [:aggregations :terms:state :buckets]))
        created  (:CREATED terms 0)
        pending  (:PENDING terms 0)]
    {:total    (:count summary)
     :started  (:STARTED terms 0)
     :created  created
     :stopped  (:STOPPED terms 0)
     :error    (:ERROR terms 0)
     :pending  pending
     :starting (+ (:STARTING terms 0) created pending)}))

(def default-states
  [{:key            :total
    :icons          [icons/i-rocket]
    :label          "TOTAL"}
   {:key            :started,
    :icons          [(utils/state->icon utils/STARTED)],
    :label          utils/STARTED,
    :positive-color "green"}
   {:key            :starting-plus,
    :icons          [(utils/state->icon utils/STARTING)],
    :label          utils/STARTING,
    :positive-color "orange"}
   {:key            :stopped,
    :icons          [(utils/state->icon utils/STOPPED)],
    :label          utils/STOPPED,
    :positive-color "orange"}
   {:key            :error,
    :icons          [(utils/state->icon utils/ERROR)],
    :label          utils/ERROR,
    :positive-color "red"}])

(defn StatisticStates
  [_clickable? summary-subs _states]
  (let [summary (subscribe [summary-subs])]
    (fn [clickable? _summary-subs states-override]
      (let [states->counts (state-aggs->state->count @summary)
            states (or states-override default-states)]
        [ui/GridColumn {:width 8}
         (into [ui/StatisticGroup {:size  "tiny"
                                   :style {:justify-content "center"}}
                (for [state states]
                  ^{:key (:key state)}
                  [components/StatisticState
                   (merge state
                     {:value                    (states->counts (:key state))
                      :stacked?                 true
                      :clickable?               (or (:clickable? state) clickable?)
                      :set-state-selector-event ::events/set-state-selector
                      :state-selector-subs      ::subs/state-selector})])])]))))

(defn TitledCardDeployments
  [& children]
  (let [tr (subscribe [::i18n-subs/tr])]
    [TitledCard {:class :nuvla-deployments
                 :icon  icons/i-rocket
                 :label (str/capitalize (@tr [:deployments]))}
     children]))

(defn DeploymentsOverviewSegment
  [{:keys [sub-key show-me-event on-click]}]
  (let [tr @(subscribe [::i18n-subs/tr])]
    [TitledCardDeployments
    ^{:key "deployment-overview-stats"}
    [StatisticStates false sub-key]
    ^{:key "deployment-overview-button"}
    [uix/Button {:class    "center"
                 :color    "blue"
                 :icon     icons/i-rocket
                 :content  (tr [:show-me])
                 :on-click (or on-click
                               #(when show-me-event (dispatch show-me-event)))}]]))

(defn Pagination
  [db-path-arg]
  (let [dep-count @(subscribe [::subs/deployments-count])]
    [pagination-plugin/Pagination
     {:db-path                [(or db-path-arg ::spec/pagination)]
      :total-items            dep-count
      :change-event           [::events/refresh db-path-arg]
      :i-per-page-multipliers [1 2 4]}]))

(defn DeploymentTable
  [options]
  (let [deployments (subscribe deployments-resources-subs-key)]
    (fn [{:keys [no-actions]}]
      (let [show-options (not (true? no-actions))]
        [:div
         [VerticalDataTable
          @deployments (assoc options :show-options? show-options)]
         (if-let [pagination (:pagination options)]
           [pagination]
           [Pagination (:pagination-db-path options)])]))))

(defn DeploymentsMainContent
  []
  (dispatch [::events/init])
  (fn []
    [components/LoadingPage {}
     [:<>
      [MenuBar]
      [ui/Grid {:stackable true
                :reversed  "mobile"
                :style     {:margin-top    0
                            :margin-bottom 0}}
       [ControlBar]
       [StatisticStates true ::subs/deployments-summary]]
      [bulk-progress-plugin/MonitoredJobs
       {:db-path [::spec/bulk-jobs]}]
      [DeploymentsDisplay]
      [Pagination]]]))


(defn deployments-view
  []
  [ui/Segment style/basic [DeploymentsMainContent]])
