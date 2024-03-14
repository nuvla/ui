(ns sixsq.nuvla.ui.edges.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.deployment-sets-detail.subs :as depl-group-subs]
            [sixsq.nuvla.ui.edges-detail.views :as edges-detail]
            [sixsq.nuvla.ui.edges.add-modal :as add-modal]
            [sixsq.nuvla.ui.edges.events :as events]
            [sixsq.nuvla.ui.edges.spec :as spec]
            [sixsq.nuvla.ui.edges.subs :as subs]
            [sixsq.nuvla.ui.edges.utils :as utils]
            [sixsq.nuvla.ui.edges.views-clusters :as views-clusters]
            [sixsq.nuvla.ui.edges.views-utils :as views-utils]
            [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :as table-plugin :refer [TableColsEditable]]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.bulk-edit-tags-modal :as bulk-edit-modal]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.map :as map]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
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
   spec/cluster-view icons/i-chart-network})

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

(defn NuvlaboxRow
  [{{:keys [id name description created state tags online
            refresh-interval version created-by owner nuvlabox-engine-version] :as nuvlabox} :row-data
    field-key                                                                                :field-key}]
  (let [uuid                  (general-utils/id->uuid id)
        locale                @(subscribe [::i18n-subs/locale])
        last-heartbeat-moment @(subscribe [::subs/last-online nuvlabox])
        engine-version        @(subscribe [::subs/engine-version id])
        creator               (subscribe [::session-subs/resolve-user created-by])
        owner                 (subscribe [::session-subs/resolve-user owner])
        releases-by-no        (subscribe [::subs/nuvlabox-releases])
        latest-release-number (:release (first @releases-by-no))
        version-warning       (when-let [nuvla-version (some #(when % %) [engine-version nuvlabox-engine-version])]
                                (let [{:keys [minor major patch] :as version-difference} (utils/version-difference latest-release-number nuvla-version)]
                                  (cond
                                    (not version-difference) nil
                                    major "update recommended"
                                    (or minor patch) "update available"
                                    :else nil)))
        field-key->table-cell {:description      description,
                               :tags             [uix/Tags tags],
                               :refresh-interval (str refresh-interval "s"),
                               :name             (or name uuid),
                               :created          (time/parse-ago created locale),
                               :state            [ui/Icon {:class (utils/state->icon state)}]
                               :online           [OnlineStatusIcon online nil true]
                               :created-by       @creator
                               :owner            @owner
                               :last-online
                               (when last-heartbeat-moment
                                 [uix/TimeAgo last-heartbeat-moment]),
                               :version          [:div
                                                  [:span {:style {:display "inline-block"
                                                                  :width 40}} (or engine-version nuvlabox-engine-version (str version ".y.z"))]
                                                  (when version-warning
                                                    [ui/Popup
                                                     {:trigger  (r/as-element [ui/Icon {:class icons/i-triangle-exclamation
                                                                                        :color (if (= version-warning "update available")
                                                                                                 "yellow"
                                                                                                 "red")}])
                                                      :content  version-warning
                                                      :position "right center"
                                                      :size     "small"}])]}]
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
    (when-not (= view-type spec/map-view)
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

(defn bulk-deploy-dynamic
  []
  (let [id (random-uuid)]
    (dispatch [::events/bulk-deploy-dynamic id])
    (dispatch [::routing-events/navigate
               routes/deployment-groups-details
               {:uuid :create}
               {depl-group-subs/creation-temp-id-key id}])))

(defn bulk-deploy-static
  []
  (let [id (random-uuid)]
    (dispatch [::events/bulk-deploy-static])
    (dispatch [::routing-events/navigate
               routes/deployment-groups-details
               {:uuid :create}
               {depl-group-subs/creation-temp-id-key id}])))

(defn state-filter-selected?
  [additional-filter state-selector]
  (or state-selector
      (some #(some-> additional-filter (str/includes? %))
            ["online=" "online!=" "state=" "state^=" "state!="])))

(defn NuvlaboxTable
  []
  (let [search-filter            (subscribe [::subs/search-filter])
        additional-filter        (subscribe [::subs/additional-filter])
        state-selector           (subscribe [::subs/state-selector])
        state-filter?            (state-filter-selected? @additional-filter @state-selector)
        nuvlaboxes               (subscribe [::subs/nuvlaboxes])
        current-cluster          (subscribe [::subs/nuvlabox-cluster])
        selected-nbs             (if @current-cluster
                                   (for [target-nb-id (concat (:nuvlabox-managers @current-cluster)
                                                              (:nuvlabox-workers @current-cluster))]
                                     (into {} (get (group-by :id (:resources @nuvlaboxes)) target-nb-id)))
                                   (:resources @nuvlaboxes))
        selection                (subscribe [::table-plugin/selected-set-sub [::spec/select]])
        maj-version-only?        (subscribe [::subs/one-edge-with-only-major-version (map :id selected-nbs)])
        tr                       (subscribe [::i18n-subs/tr])
        all-selected?            (subscribe [::table-plugin/select-all?-sub [::spec/select]])
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
        {bulk-edit-modal :modal
         trigger         :trigger-config} bulk-edit
        bulk-deploy-menuitem     {:menuitem (let [message         (@tr [:deploy-with-static-edges])
                                                  deploy-menuitem [uix/HighlightableMenuItem
                                                                   {:on-click          bulk-deploy-static
                                                                    :query-param-value :bulk-deploy}
                                                                   [icons/RocketIcon]
                                                                   (@tr [:edges-bulk-deploy-app])]]
                                              ^{:key "bulk-deploy-menuitem"}
                                              [ui/Popup {:basic   true
                                                         :content message
                                                         :trigger (r/as-element [:div deploy-menuitem])}])}
        dyn-bulk-deploy-menuitem {:menuitem (let [dynamic-bulk-deploy-enabled? (and (not (seq @selection))
                                                                                    (not @search-filter)
                                                                                    (not state-filter?))
                                                  message                      (str (@tr [:deploy-with-edges-filter])
                                                                                    "\n"
                                                                                    (if (or @search-filter @additional-filter)
                                                                                      (utils/get-deploy-filter-string @search-filter @additional-filter)
                                                                                      (@tr [:deploy-with-catch-all-edges-filter])))
                                                  wrong-filter-message         (cond
                                                                                 (or (seq @selection) @all-selected?)
                                                                                 (@tr [:deploy-with-edges-clear-selection])
                                                                                 @search-filter
                                                                                 (@tr [:deploy-with-edges-fulltext-filter-not-allowed])
                                                                                 state-filter?
                                                                                 (@tr [:deploy-with-edges-state-filter-not-allowed]))
                                                  deploy-menuitem              [uix/HighlightableMenuItem
                                                                                {:disabled          (not dynamic-bulk-deploy-enabled?)
                                                                                 :on-click          bulk-deploy-dynamic
                                                                                 :query-param-value :dynamic-bulk-deploy}
                                                                                [icons/RocketIcon]
                                                                                (@tr [:dynamic-bulk-deploy])]]
                                              ^{:key "dyn-bulk-deploy-menuitem"}
                                              [ui/Popup {:basic   true
                                                         :content (if dynamic-bulk-deploy-enabled?
                                                                    message
                                                                    wrong-filter-message)
                                                         :trigger (r/as-element [:div deploy-menuitem])}])}]
    [:<>
     (when bulk-edit-modal [bulk-edit-modal])
     [NuvlaEdgeTableView {:select-config {:bulk-actions        (filterv
                                                                 some?
                                                                 [trigger
                                                                  bulk-deploy-menuitem
                                                                  dyn-bulk-deploy-menuitem])
                                          :total-count-sub-key [::subs/nuvlaboxes-count]
                                          :resources-sub-key   [::subs/nuvlaboxes-resources]
                                          :select-db-path      [::spec/select]
                                          :rights-needed       :edit}
                          :columns       columns :edges selected-nbs
                          :filter-fn     (partial utils/build-bulk-filter [::spec/select])}]]))


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
        (condp = @view-type
          spec/cards-view [NuvlaboxCards]
          spec/table-view [NuvlaboxTable]
          spec/map-view [NuvlaboxMap]
          spec/cluster-view [views-clusters/NuvlaboxClusters]
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
