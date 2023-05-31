(ns sixsq.nuvla.ui.deployments.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.deployment-dialog.views-module-version :as dep-diag-versions]
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
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [Table]]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.bulk-edit-tags-modal :as bulk-edit-modal]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

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
          {:resource-name  spec/resource-name
           :default-filter @additional-filter
           :open?          filter-open?
           :on-done        #(dispatch [::events/set-additional-filter %])
           :show-clear-button-outside-modal? true}]]]])))

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

(defn BulkActionModal
  [{:keys [on-confirm trigger header danger-msg button-text]}]
  (let [open? (r/atom false)
        close        (fn []
                       (swap! open? not))]
    (fn []
      [uix/ModalDanger
       {:on-confirm  #(do
                        (on-confirm)
                        (reset! open? false))
        :open        @open?
        :on-close    close
        :trigger     (r/as-element
                      [:div {:on-click close} trigger])
        :header      header
        :danger-msg  danger-msg
        :button-text button-text}])))

(defn BulkStopModal
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [BulkActionModal
       {:on-confirm   (fn [] (dispatch [::events/bulk-operation "bulk-stop"]))
        :trigger     (str/capitalize (@tr [:stop]))
        :header      (@tr [:bulk-deployment-stop])
        :danger-msg  (@tr [:danger-action-cannot-be-undone])
        :button-text (str/capitalize (@tr [:bulk-deployment-stop]))}])))

(defn BulkForceDeleteModal
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [BulkActionModal
       {:on-confirm  (fn [] (dispatch [::events/bulk-operation "bulk-force-delete"]))
        :trigger     (str/capitalize (@tr [:force-delete]))
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
         [ui/MenuItem {:icon     "grid layout"
                       :active   (= @view "cards")
                       :on-click #(dispatch [::events/set-view "cards"])}]
         [ui/MenuItem {:icon     "table"
                       :active   (= @view "table")
                       :on-click #(dispatch [::events/set-view "table"])}]

         [components/RefreshMenu
          {:action-id  events/refresh-action-deployments-id
           :on-refresh refresh}]]]])))


(defn RowFn
  [{:keys [id state module tags created-by] :as deployment}
   {:keys [no-module-name show-options?] :as _options}]
  (let [[primary-url-name
         primary-url-pattern] (-> module :content (get :urls []) first)
        url       @(subscribe [::subs/deployment-url id primary-url-pattern])
        creator   (subscribe [::session-subs/resolve-user created-by])]
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
     [ui/TableCell (-> deployment :created time/parse-iso8601 time/ago)]
     [ui/TableCell @creator]
     [ui/TableCell [uix/Tags tags]]
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
  (let [tr                (subscribe [::i18n-subs/tr])]
    (fn [deployments-list {:keys [show-options? no-module-name empty-msg] :as options}]
      (if (empty? deployments-list)
        [uix/WarningMsgNoElements empty-msg]
        (let [selectable?                (or (nil? show-options?) show-options?)
              {trigger :trigger-config
               BulkEditTagsModal :modal} (bulk-edit-modal/create-bulk-edit-modal
                                           {:db-path                [::spec/select]
                                            :refetch-event          ::events/get-deployments
                                            :resource-key           :deployment
                                            :total-count-sub-key    ::subs/deployments-count
                                            :on-open-modal-event    ::events/get-deployments-without-edit-rights
                                            :no-edit-rights-sub-key ::subs/deployments-without-edit-rights
                                            :singular               (@tr [:deployment])
                                            :plural                 (@tr [:deployments])
                                            :filter-fn               build-bulk-filter})]
          [:<>
           [BulkEditTagsModal]
           [Table {:columns      [{:field-key :id}
                                  (when-not no-module-name
                                    {:field-key      :module.name
                                     :header-content (@tr [:module])})
                                  {:field-key :version :no-sort? true}
                                  {:field-key :status
                                   :sort-key  :state}
                                  {:field-key :url
                                   :no-sort?  true}
                                  {:field-key :created}
                                  {:field-key :created-by}
                                  {:field-key :tags}
                                  {:field-key :infrastructure
                                   :no-sort?  true}
                                  (when selectable? {:field-key :actions
                                                     :no-sort?  true})]
                   :rows         deployments-list
                   :sort-config   {:db-path     ::spec/ordering
                                   :fetch-event (or (:fetch-event options) [::events/get-deployments])}
                   :row-render   (fn [deployment] [RowFn deployment options])
                   :table-props  (merge style/single-line {:stackable true})
                   :select-config (when selectable?
                                    {:bulk-actions [{:event [::events/bulk-update-params]
                                                     :name (str/capitalize (@tr [:update]))}
                                                    {:component (r/as-element BulkStopModal)}
                                                    {:component (r/as-element BulkForceDeleteModal)}
                                                    trigger]
                                     :select-db-path [::spec/select]
                                     :total-count-sub-key [::subs/deployments-count]
                                     :resources-sub-key deployments-resources-subs-key
                                     :rights-needed :edit})}]])))))

(defn DeploymentCard
  [{:keys [id state module tags created-by] :as deployment}]
  (let [tr           (subscribe [::i18n-subs/tr])
        {module-logo-url :logo-url
         module-name     :name
         module-content  :content} module
        [primary-url-name
         primary-url-pattern] (-> module-content (get :urls []) first)
        primary-url  (subscribe [::subs/deployment-url id primary-url-pattern])
        started?     (utils/started? state)
        creator      (subscribe [::session-subs/resolve-user created-by])]
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
       (if (= @view "cards")
         [CardsDataTable @deployments]
         [VerticalDataTable @deployments])])))

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
          [components/StatisticState {:value                    total
                                      :icons                    ["fa-light fa-rocket-launch"]
                                      :label                    "TOTAL"
                                      :stacked?                 true
                                      :clickable?               clickable?
                                      :set-state-selector-event ::events/set-state-selector
                                      :state-selector-subs      ::subs/state-selector}]
          [components/StatisticState {:value                    started,
                                      :icons                    [(utils/state->icon utils/STARTED)],
                                      :label                    utils/STARTED,
                                      :stacked?                 true
                                      :clickable?               clickable?,
                                      :positive-color           "green",
                                      :set-state-selector-event ::events/set-state-selector,
                                      :state-selector-subs      :sixsq.nuvla.ui.deployments.subs/state-selector}]
          [components/StatisticState {:value                    starting-plus,
                                      :icons                    [(utils/state->icon utils/STARTING)],
                                      :label                    utils/STARTING,
                                      :stacked?                 true
                                      :clickable?               clickable?,
                                      :positive-color           "orange",
                                      :set-state-selector-event ::events/set-state-selector,
                                      :state-selector-subs      :sixsq.nuvla.ui.deployments.subs/state-selector}]
          [components/StatisticState {:value                    stopped,
                                      :icons                    [(utils/state->icon utils/STOPPED)],
                                      :label                    utils/STOPPED,
                                      :stacked?                 true
                                      :clickable?               clickable?,
                                      :positive-color           "orange",
                                      :set-state-selector-event ::events/set-state-selector,
                                      :state-selector-subs      :sixsq.nuvla.ui.deployments.subs/state-selector}]
          [components/StatisticState {:value                    error,
                                      :icons                    [(utils/state->icon utils/ERROR)],
                                      :label                    utils/ERROR,
                                      :stacked?                 true
                                      :clickable?               clickable?,
                                      :positive-color           "red",
                                      :set-state-selector-event ::events/set-state-selector,
                                      :state-selector-subs      :sixsq.nuvla.ui.deployments.subs/state-selector}]]]))))

(defn DeploymentsOverviewSegment
  [deployment-subs set-active-tab-event deployment-tab-key on-click]
  (let [tr    (subscribe [::i18n-subs/tr])
        icon  "rocket"]
    [ui/Segment {:class     :nuvla-deployments
                 :secondary true
                 :raised    true
                 :style     {:display         "flex"
                             :flex-direction   "column"
                             :justify-content "space-between"}}

     [:h4 {:class :ui-header
           :style {:border-radius ".28571429rem .28571429rem 0 0"}}
      [ui/Icon {:name icon}] (str/capitalize (@tr [:deployments]))]

     [StatisticStates false deployment-subs]

     [ui/Button {:class    "center"
                 :style    {:align-self "start"}
                 :content  "Show me"
                 :on-click (or on-click
                               #(dispatch [set-active-tab-event deployment-tab-key]))}]]))

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
  (let [deployments    (subscribe deployments-resources-subs-key)]
    (fn [{:keys [no-actions]}]
      (let [show-options (not (true? no-actions))]
        [:div
         [VerticalDataTable
          @deployments (assoc options :show-options? show-options)]
         [Pagination (:pagination-db-path options)]]))))

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
