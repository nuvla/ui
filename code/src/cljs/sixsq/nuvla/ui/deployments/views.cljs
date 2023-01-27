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
            [sixsq.nuvla.ui.deployments.utils :as utils]
            [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [Table]]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

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
          {:resource-name  "deployment"
           :default-filter @additional-filter
           :open?          filter-open?
           :on-done        #(dispatch [::events/set-additional-filter %])}]]]])))

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


(defn RowFn
  [{:keys [id state module created-by] :as deployment}
   {:keys [no-module-name show-options?] :as _options}]
  (let [[primary-url-name
         primary-url-pattern] (-> module :content (get :urls []) first)
        url       @(subscribe [::subs/deployment-url id primary-url-pattern])
        selected? (subscribe [::subs/is-selected? id])
        creator   (subscribe [::session-subs/resolve-user created-by])]
    [ui/TableRow
     (when show-options?
       [ui/TableCell
        [ui/Checkbox {:checked  @selected?
                      :on-click (fn [event]
                                  (dispatch [::events/select-id id])
                                  (.stopPropagation event))}]])
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
    (fn [deployments-list {:keys [show-options? no-module-name empty-msg] :as options}]
      (if (empty? deployments-list)
        [uix/WarningMsgNoElements empty-msg]
        [Table {:columns     [(when show-options?
                                {:no-sort? true
                                 :header-content
                                 [ui/Checkbox
                                  {:checked  @is-all-page-selected?
                                   :on-click #(dispatch [::events/select-all-page])}]})
                              {:field-key :id}
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
                              {:field-key :infrastructure
                               :no-sort?  true}
                              (when show-options? {:field-key :actions
                                                   :no-sort?  true})]
                :rows        deployments-list
                :sort-config {:db-path     ::spec/ordering
                              :fetch-event ::events/get-deployments}
                :row-render  (fn [deployment] [RowFn deployment options])
                :table-props (merge style/single-line {:stackable true})}]))))


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
        select-all?  (subscribe [::subs/select-all?])
        creator      (subscribe [::session-subs/resolve-user created-by])
        is-selected? (subscribe [::subs/is-selected? id])]
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
              :href          (utils/deployment-href id)
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
        [ui/Segment {:basic true :class "table-wrapper"}
         (if (= @view "cards")
           [CardsDataTable deployments-list]
           [VerticalDataTable deployments-list {:show-options? (false? @select-all?)}])]))))

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
                                      :set-state-selector-event :sixsq.nuvla.ui.deployments.events/set-state-selector,
                                      :state-selector-subs      :sixsq.nuvla.ui.deployments.subs/state-selector}]
          [components/StatisticState {:value                    starting-plus,
                                      :icons                    [(utils/state->icon utils/STARTING)],
                                      :label                    utils/STARTING,
                                      :stacked?                 true
                                      :clickable?               clickable?,
                                      :positive-color           "orange",
                                      :set-state-selector-event :sixsq.nuvla.ui.deployments.events/set-state-selector,
                                      :state-selector-subs      :sixsq.nuvla.ui.deployments.subs/state-selector}]
          [components/StatisticState {:value                    stopped,
                                      :icons                    [(utils/state->icon utils/STOPPED)],
                                      :label                    utils/STOPPED,
                                      :stacked?                 true
                                      :clickable?               clickable?,
                                      :positive-color           "orange",
                                      :set-state-selector-event :sixsq.nuvla.ui.deployments.events/set-state-selector,
                                      :state-selector-subs      :sixsq.nuvla.ui.deployments.subs/state-selector}]
          [components/StatisticState {:value                    error,
                                      :icons                    [(utils/state->icon utils/ERROR)],
                                      :label                    utils/ERROR,
                                      :stacked?                 true
                                      :clickable?               clickable?,
                                      :positive-color           "red",
                                      :set-state-selector-event :sixsq.nuvla.ui.deployments.events/set-state-selector,
                                      :state-selector-subs      :sixsq.nuvla.ui.deployments.subs/state-selector}]]]))))

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
  [db-path-arg]
  (let [dep-count @(subscribe [::subs/deployments-count])]
    [pagination-plugin/Pagination
     {:db-path                [(or db-path-arg ::spec/pagination)]
      :total-items            dep-count
      :change-event           [::events/refresh db-path-arg]
      :i-per-page-multipliers [1 2 4]}]))

(defn DeploymentTable
  [options]
  (let [elements    (subscribe [::subs/deployments])
        select-all? (subscribe [::subs/select-all?])]
    (fn [{:keys [no-actions]}]
      (let [deployments  (:resources @elements)
            show-options (and (false? @select-all?) (not (true? no-actions)))]
        [:div {:class "table-wrapper"}
         [VerticalDataTable
          deployments (assoc options :select-all @select-all? :show-options? show-options)]
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
