(ns sixsq.nuvla.ui.pages.edges.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.filter-comp.views :as filter-comp]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.common-components.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.common-components.plugins.table :as table-plugin :refer [TableColsEditable]]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.pages.edges-detail.views :as edges-detail]
            [sixsq.nuvla.ui.pages.edges.add-modal :as add-modal]
            [sixsq.nuvla.ui.pages.edges.bulk-update-modal :as bulk-update-modal]
            [sixsq.nuvla.ui.pages.edges.events :as events]
            [sixsq.nuvla.ui.pages.edges.spec :as spec]
            [sixsq.nuvla.ui.pages.edges.subs :as subs]
            [sixsq.nuvla.ui.pages.edges.utils :as utils]
            [sixsq.nuvla.ui.pages.edges.views-clusters :as views-clusters]
            [sixsq.nuvla.ui.pages.edges.views-timeseries :as views-timeseries]
            [sixsq.nuvla.ui.pages.edges.views-utils :as views-utils]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.common-components.plugins.bulk-edit-tags-modal :as bulk-edit-modal]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.map :as map]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.tooltip :as tt]
            [sixsq.nuvla.ui.utils.view-components :refer [OnlineStatusIcon]]))

(def show-state-statistics (r/atom false))


(defn switch-view!
  [new-view]
  (dispatch [::events/change-view-type new-view]))

(def edges-states
  [{:key            :total
    :icons          [icons/i-box]
    :label          "TOTAL"
    :positive-color nil}
   {:key            :online
    :icons          [icons/i-power]
    :label          utils/status-online
    :positive-color "green"}
   {:key            :offline
    :icons          [icons/i-power]
    :label          utils/status-offline
    :positive-color "red"}
   {:key            :unknown
    :icons          [icons/i-power]
    :label          utils/status-unknown
    :positive-color "orange"}])

(defn StatisticStatesEdgeView
  []
  (fn [{:keys [states] :as states->counts} clickable? restricted-view?]
    (let [tr (subscribe [::i18n-subs/tr])]
      [ui/StatisticGroup {:widths (when-not clickable? 4)
                          :size   "tiny"}
       (for [state (or states edges-states)]
         ^{:key (str "stat-state-" (:label state))}
         [components/StatisticState
          (merge state
                 {:value                    (states->counts (:key state))
                  :stacked?                 true
                  :clickable?               (or (:clickable? state) clickable?)
                  :set-state-selector-event ::events/set-state-selector
                  :state-selector-subs      ::subs/state-selector})])
       (when (and clickable? (not restricted-view?))
         [ui/Button
          {:icon     true
           :style    {:margin "50px auto 15px"}
           :on-click #(when clickable?
                        (reset! show-state-statistics (not @show-state-statistics))
                        (when-not @show-state-statistics
                          (dispatch [::events/set-state-selector nil])))}
          [icons/ArrowDownIcon]
          \u0020
          (@tr [:commissionning-states])])])))


(defn StatisticStatesEdge
  [clickable?]
  (let [summary (if clickable?
                  (subscribe [::subs/nuvlaboxes-summary])
                  (subscribe [::subs/nuvlaboxes-summary-all]))]
    (fn []
      (let [total           (:count @summary)
            online-statuses (general-utils/aggregate-to-map
                              (get-in @summary [:aggregations :terms:online :buckets]))
            online          (:1 online-statuses)
            offline         (:0 online-statuses)]
        [StatisticStatesEdgeView
         {:total   total
          :online  online
          :offline offline
          :unknown (- total (+ online offline))}
         clickable?]))))

(defn StatisticStates
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        summary  (subscribe [::subs/nuvlaboxes-summary])
        selected (subscribe [::subs/state-selector])]
    (when ((set utils/states) @selected)
      (reset! show-state-statistics true))
    (fn []
      (let [terms (general-utils/aggregate-to-map
                    (get-in @summary [:aggregations :terms:state :buckets]))]
        [:div {:style {:display         :flex
                       :justify-content :center
                       :flex-direction  :column
                       :align-items     :center}}
         [StatisticStatesEdge true]
         [ui/Segment {:compact true
                      :width   "auto"
                      :style   {:text-align "center"
                                :display    (if @show-state-statistics "table" "none")}}
          [:h4 (@tr [:commissionning-states])]
          [ui/StatisticGroup
           {:size  "tiny"
            :style {:margin     "10px auto 10px auto"
                    :display    "flex"
                    :text-align "center"
                    :width      "100%"}}
           (for [state utils/states]
             ^{:key state}
             [components/StatisticState
              {:value                    ((keyword state) terms 0)
               :icons                    [(utils/state->icon state)]
               :label                    state
               :clickable?               true
               :set-state-selector-event ::events/set-state-selector
               :state-selector-subs      ::subs/state-selector
               :stacked?                 true}])]]]))))

(def view->icon-classes
  {spec/cards-view   icons/i-grid-layout
   spec/table-view   icons/i-table
   spec/map-view     icons/i-map
   spec/cluster-view icons/i-chart-network
   spec/history-view icons/i-history})

(defn MenuBar []
  (let [loading?  (subscribe [::subs/loading?])
        view-type (subscribe [::subs/view-type])]
    (fn []
      [components/StickyBar
       [ui/Menu {:borderless true, :stackable true}
        [views-utils/AddButton]
        (doall
          (for [view spec/view-types]

            ^{:key view}
            [ui/MenuItem {:active   (= @view-type view)
                          :on-click #(switch-view! view)}
             [icons/Icon {:name (view->icon-classes view)}]]))
        [components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [::events/refresh-root])}]]])))


(defn AddModalWrapper
  []
  (let [nb-release (subscribe [::subs/nuvlabox-releases])]
    ^{:key (count @nb-release)} [add-modal/AddModal]))

(defn NEVersion
  [{:keys [id version nuvlabox-engine-version] :as _nuvlabox}]
  (let [engine-version  @(subscribe [::subs/engine-version id])
        ne-version      (or engine-version nuvlabox-engine-version)
        version-warning (when ne-version
                          @(subscribe [::subs/ne-version-outdated ne-version]))]
    [utils/NEVersionWarning version-warning
     (fn [Icon]
       [:span
        (or ne-version (str version ".y.z"))
        " "
        Icon])]))

(defn NuvlaboxRow
  [{{:keys [id name description created state tags online
            refresh-interval created-by owner] :as nuvlabox} :row-data
    field-key                                                :field-key}]
  (let [uuid                  (general-utils/id->uuid id)
        last-online           @(subscribe [::subs/last-online nuvlabox])
        creator               (subscribe [::session-subs/resolve-user created-by])
        owner                 (subscribe [::session-subs/resolve-user owner])
        field-key->table-cell {:description      [tt/WithOverflowTooltip
                                                  {:content description
                                                   :tooltip description}],
                               :tags             [uix/Tags tags],
                               :refresh-interval (str refresh-interval "s"),
                               :name             [tt/WithOverflowTooltip
                                                  {:content (or name uuid)
                                                   :tooltip (or name uuid)}],
                               :created          [uix/TimeAgo created]
                               :state            [ui/Icon {:class (utils/state->icon state)}]
                               :online           [OnlineStatusIcon online nil true]
                               :created-by       @creator
                               :owner            @owner
                               :last-online
                               (when last-online
                                 [uix/TimeAgo last-online]),
                               :version          [NEVersion nuvlabox]}]
    (field-key->table-cell field-key)))

(defn Pagination
  [view-type]
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        nuvlabox-clusters (subscribe [::subs/nuvlabox-clusters])
        current-cluster   (subscribe [::subs/nuvlabox-cluster])
        total-elements    (if (= view-type spec/cluster-view)
                            (:count @nuvlabox-clusters)
                            (if @current-cluster
                              (+ (count (:nuvlabox-managers @current-cluster))
                                 (count (:nuvlabox-managers @current-cluster)))
                              (:count @nuvlaboxes)))]
    (when-not (#{spec/map-view spec/history-view} view-type)
      [pagination-plugin/Pagination
       {:db-path                [::spec/pagination]
        :change-event           [::events/refresh-root]
        :total-items            total-elements
        :i-per-page-multipliers [1 2 4]}])))

(defn NuvlaEdgeTableView
  [{:keys [columns edges select-config sort-config]}]
  [TableColsEditable
   {:cols-without-rmv-icon #{:state :online}
    :sort-config           (or sort-config {:db-path     ::spec/ordering
                                            :fetch-event [::events/get-nuvlaboxes]})
    :columns               columns
    :default-columns       #{:online :state :name :last-online :version :tags}
    :rows                  edges
    :table-props           {:compact "very" :selectable true}
    :cell-props            {:header {:single-line true}}
    :row-click-handler     (fn [{id :id}] (dispatch [::routing-events/navigate (utils/edges-details-url (general-utils/id->uuid id))]))
    :row-props             {:role  "link"
                            :style {:cursor "pointer"}}
    :row-props-fn          {}
    :select-config         select-config}
   ::table-cols-config])

(defn state-filter-selected?
  [additional-filter state-selector]
  (or state-selector
      (some #(some-> additional-filter (str/includes? %))
            ["online=" "online!=" "state=" "state^=" "state!="])))

(defn NuvlaboxTable
  []
  (r/with-let [search-filter     (subscribe [::subs/search-filter])
               additional-filter (subscribe [::subs/additional-filter])
               state-selector    (subscribe [::subs/state-selector])
               nuvlaboxes        (subscribe [::subs/nuvlaboxes])
               current-cluster   (subscribe [::subs/nuvlabox-cluster])
               selection         (subscribe [::table-plugin/selected-set-sub [::spec/select]])
               tr                (subscribe [::i18n-subs/tr])
               all-selected?     (subscribe [::table-plugin/select-all?-sub [::spec/select]])
               bulk-update-state (bulk-update-modal/init-state)]
    (let [state-filter?            (state-filter-selected? @additional-filter @state-selector)
          selected-nbs             (if @current-cluster
                                     (for [target-nb-id (concat (:nuvlabox-managers @current-cluster)
                                                                (:nuvlabox-workers @current-cluster))]
                                       (into {} (get (group-by :id (:resources @nuvlaboxes)) target-nb-id)))
                                     (:resources @nuvlaboxes))
          maj-version-only?        (subscribe [::subs/one-edge-with-only-major-version (map :id selected-nbs)])
          columns                  (mapv (fn [col-config]
                                           (assoc col-config :cell NuvlaboxRow))
                                         [{:field-key :online :header-content [icons/HeartbeatIcon] :cell-props {:collapsing true}}
                                          {:field-key :state :cell-props {:collapsing true}}
                                          {:field-key :name}
                                          {:field-key :description}
                                          {:field-key :created}
                                          {:field-key :created-by}
                                          {:field-key :owner}
                                          {:field-key      :refresh-interval
                                           :header-content (str/lower-case (@tr [:telemetry]))}
                                          {:field-key :last-online :no-sort? true}
                                          {:field-key      :version :no-sort? true
                                           :header-content [:<> (@tr [:version])
                                                            (when @maj-version-only? [uix/HelpPopup (@tr [:edges-version-info])])]}
                                          {:field-key :tags :no-sort? true}])
          bulk-edit                (bulk-edit-modal/create-bulk-edit-modal
                                     {:db-path                [::spec/select]
                                      :refetch-event          ::events/get-nuvlaboxes
                                      :resource-key           :nuvlabox
                                      :total-count-sub-key    ::subs/nuvlaboxes-count
                                      :on-open-modal-event    ::events/get-edges-without-edit-rights
                                      :no-edit-rights-sub-key ::subs/edges-without-edit-rights
                                      :singular               (@tr [:edge])
                                      :plural                 (@tr [:edges])
                                      :filter-fn              (partial utils/build-bulk-filter [::spec/select])})
          {bulk-edit-modal         :modal
           bulk-edit-tags-menuitem :trigger-config} bulk-edit
          bulk-update-menuitem     {:icon  icons/DownloadIcon
                                    :key   :bulk-update
                                    :name  "Bulk update" #_(@tr [:edit-tags])
                                    :event (partial bulk-update-modal/open-modal bulk-update-state)}]
      [:<>
       (when bulk-edit-modal [bulk-edit-modal])
       (when @(bulk-update-modal/open? bulk-update-state)
         [bulk-update-modal/Modal bulk-update-state])

       [NuvlaEdgeTableView {:select-config {:bulk-actions        [bulk-edit-tags-menuitem
                                                                  bulk-update-menuitem]
                                            :total-count-sub-key [::subs/nuvlaboxes-count]
                                            :resources-sub-key   [::subs/nuvlaboxes-resources]
                                            :select-db-path      [::spec/select]
                                            :rights-needed       :edit}
                            :columns       columns :edges selected-nbs
                            :filter-fn     (partial utils/build-bulk-filter [::spec/select])}]])))


(defn NuvlaboxMapPoint
  [{:keys [id name location inferred-location online]}]
  (let [uuid     (general-utils/id->uuid id)
        on-click #(dispatch [::routing-events/navigate (utils/edges-details-url uuid)])]
    [map/CircleMarker {:on-click on-click
                       :center   (map/longlat->latlong (or location inferred-location))
                       :color    (utils/map-online->color online)
                       :opacity  0.5
                       :weight   1
                       :radius   7}
     [map/Tooltip (or name id)]]))


(defn NuvlaboxCards
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        nuvlabox-clusters (subscribe [::subs/nuvlabox-clusters])
        managers          (distinct
                            (apply concat
                                   (map :nuvlabox-managers (:resources @nuvlabox-clusters))))
        selected-nbs      (:resources @nuvlaboxes)]
    [:div style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4}
      (for [{:keys [id] :as nuvlabox} selected-nbs]
        (when id
          ^{:key id}
          [views-utils/NuvlaboxCard nuvlabox managers]))]]))


(defn NuvlaboxMap
  []
  (let [nuvlabox-locations (subscribe [::subs/nuvlabox-locations])
        nbs-locations      (:resources @nuvlabox-locations)]
    [map/MapBox
     {:responsive-height? true}
     (doall
       (for [{:keys [id] :as nuvlabox} nbs-locations]
         ^{:key id}
         [NuvlaboxMapPoint nuvlabox]))]))


(defn- ControlBar
  []
  (let [additional-filter (subscribe [::subs/additional-filter])
        filter-open?      (r/atom false)]
    (fn []
      [ui/GridColumn {:width 4}
       [:div {:style {:display :flex}}
        [:div
         [full-text-search-plugin/FullTextSearch
          {:db-path            [::spec/edges-search]
           :change-event       [::pagination-plugin/change-page
                                [::spec/pagination] 1]
           :placeholder-suffix (str " " @(subscribe [::subs/state-selector]))
           :style              {:width "100%"}}]
         ^{:key (random-uuid)}
         [:div {:style {:margin-top "10px"}}
          [filter-comp/ButtonFilter
           {:resource-name                    spec/resource-name
            :default-filter                   @additional-filter
            :open?                            filter-open?
            :on-done                          #(dispatch [::events/set-additional-filter %])
            :show-clear-button-outside-modal? true}]]]]])))


(defn NuvlaBoxesOrClusters
  [external-restriction-filter]
  (dispatch [::events/init external-restriction-filter])
  (dispatch [::events/set-nuvlabox-cluster nil])
  (let [view-type (subscribe [::subs/view-type])]
    (fn []
      [components/LoadingPage {}
       [:<>
        [MenuBar]
        [ui/Grid {:stackable true
                  :reversed  "mobile"
                  :style     {:margin-top    0
                              :margin-bottom 0}}
         [ControlBar]
         [ui/GridColumn {:width 10}
          (if (= @view-type spec/cluster-view)
            [views-clusters/StatisticStates]
            [StatisticStates])]]
        [bulk-progress-plugin/MonitoredJobs
         {:db-path [::spec/bulk-jobs]}]
        (condp = @view-type
          spec/cards-view [NuvlaboxCards]
          spec/table-view [NuvlaboxTable]
          spec/map-view [NuvlaboxMap]
          spec/cluster-view [views-clusters/NuvlaboxClusters]
          spec/history-view [views-timeseries/FleetTimeSeries]
          [NuvlaboxTable])
        [Pagination @view-type]]])))


(defn DetailedViewPage
  [{{:keys [uuid]} :path-params}]
  (if (= "nuvlabox-cluster" uuid)
    (do
      (switch-view! spec/cluster-view)
      (dispatch [::routing-events/navigate routes/edges]))
    [edges-detail/EdgeDetails uuid]))


(defn edges-view
  []
  [:<>
   [ui/Segment style/basic [NuvlaBoxesOrClusters]]
   [AddModalWrapper]])
