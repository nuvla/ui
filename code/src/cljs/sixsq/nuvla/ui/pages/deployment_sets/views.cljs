(ns sixsq.nuvla.ui.pages.deployment-sets.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor :refer [TableController]]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.pages.deployment-sets-detail.views :as detail]
            [sixsq.nuvla.ui.pages.deployment-sets.events :as events]
            [sixsq.nuvla.ui.pages.deployment-sets.spec :as spec]
            [sixsq.nuvla.ui.pages.deployment-sets.subs :as subs]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as routing-subs]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]))

(def view-type (r/atom :table))

(def ^:const NEW "NEW")
(def ^:const STARTED "STARTED")
(def ^:const UPDATED "UPDATED")
(def ^:const STOPPED "STOPPED")
(def ^:const PENDING "PENDING")
(def ^:const PARTIAL "PARTIAL")

(defn state->icon
  [state]
  (cond
    (str/ends-with? state "ING") icons/i-sync
    (str/starts-with? state "PARTIAL") icons/i-adjust
    :else (get {NEW     icons/i-sticky-note
                STARTED icons/i-play
                UPDATED icons/i-play
                STOPPED icons/i-stop} state)))

(defn AutoUpdateIcon
  []
  [icons/Icon {:name "fal fa-clock-rotate-left" :style {:margin-right 0}}])

(defn StatisticStates
  [clickable?]
  (let [summary           (subscribe [::subs/deployment-sets-summary])
        terms             (general-utils/aggregate-to-map
                            (get-in @summary [:aggregations :terms:state :buckets]))
        new               (:NEW terms 0)
        started           (+ (:STARTED terms 0)
                             (:UPDATED terms 0))
        starting          (:STARTING terms 0)
        updating          (:UPDATING terms 0)
        stopping          (:STOPPING terms 0)
        stopped           (:STOPPED terms 0)
        pending           (+ starting stopping updating)
        partially-stopped (:PARTIALLY-STOPPED terms 0)
        partially-started (:PARTIALLY-STARTED terms 0)
        partially-updated (:PARTIALLY-UPDATED terms 0)
        partial           (+ partially-stopped partially-started partially-updated)
        total             (:count @summary)]
    [ui/GridColumn {:width 8}
     [ui/StatisticGroup {:size  "tiny"
                         :style {:justify-content "center"}}
      (for [stat-props [{:value          total
                         :icons          [icons/i-bullseye]
                         :label          "TOTAL"
                         :positive-color nil}
                        {:value          new
                         :icons          [(str "fal " (state->icon NEW))]
                         :label          NEW
                         :positive-color "black"}
                        {:value          started
                         :icons          [(str "fal " (state->icon STARTED))]
                         :label          STARTED
                         :positive-color "green"}
                        {:value          stopped
                         :icons          [(str "fal " (state->icon STOPPED))]
                         :label          STOPPED
                         :positive-color "black"}
                        {:value          pending
                         :icons          [(str "fal " (state->icon PENDING))]
                         :label          PENDING
                         :positive-color "brown"}
                        {:value          partial
                         :icons          [(str "fal " (state->icon PARTIAL))]
                         :label          PARTIAL
                         :positive-color "red"}]]
        ^{:key (str "stat-" (:label stat-props))}
        [components/StatisticState
         (assoc stat-props
           :stacked? true
           :clickable? clickable?
           :set-state-selector-event :sixsq.nuvla.ui.pages.deployment-sets.events/set-state-selector
           :state-selector-subs :sixsq.nuvla.ui.pages.deployment-sets.subs/state-selector)])]]))

(defn MenuBar []
  (let [loading? (subscribe [::subs/loading?])
        tr       (subscribe [::i18n-subs/tr])]
    (fn []
      [components/StickyBar
       [ui/Menu {:borderless true :stackable true}
        [uix/MenuItem
         {:name     (@tr [:add])
          :icon     icons/i-plus-large
          :on-click #(dispatch [::events/new-deployment-set])}]
        [ui/MenuItem {:icon     icons/i-grid-layout
                      :active   (= @view-type :cards)
                      :on-click #(reset! view-type :cards)}]
        [ui/MenuItem {:icon     "table"
                      :active   (= @view-type :table)
                      :on-click #(reset! view-type :table)}]
        [components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [::events/refresh])}]]])))

(defn Pagination
  []
  (let [deployment-sets @(subscribe [::subs/deployment-sets])]
    [pagination-plugin/Pagination {:db-path      [::spec/pagination]
                                   :total-items  (get deployment-sets :count 0)
                                   :change-event [::events/refresh]}]))

(defn CellName
  [_cell-data {:keys [uuid name] :as _deployment-set} _column]
  (or name uuid))

(defn CellDescription
  [description _row _column]
  description)

(defn CellState
  [state _row _column]
  state)

(defn CellCreated
  [created _row _column]
  [uix/TimeAgo created])

(defn CellAutoUpdate
  [auto-update _row _column]
  (when auto-update [AutoUpdateIcon]))

(defn CellTags
  [tags _row _column]
  [uix/Tags tags])

(defn DeploymentSetTable
  []
  (r/with-let [!resources      (subscribe [::subs/deployment-sets-resources])
               !pagination     (r/atom {:page-index 0
                                        :page-size  25})]
    [TableController {:!columns               (r/atom [{::table-refactor/field-key      :name
                                                        ::table-refactor/header-content "Name"
                                                        ::table-refactor/field-cell     CellName}
                                                       {::table-refactor/field-key      :description
                                                        ::table-refactor/header-content "Description"
                                                        ::table-refactor/field-cell     CellDescription}
                                                       {::table-refactor/field-key      :state
                                                        ::table-refactor/header-content "State"
                                                        ::table-refactor/field-cell     CellState}
                                                       {::table-refactor/field-key      :created
                                                        ::table-refactor/header-content "Created"
                                                        ::table-refactor/field-cell     CellCreated}
                                                       {::table-refactor/field-key      :auto-update
                                                        ::table-refactor/header-content "Auto update"
                                                        ::table-refactor/field-cell     CellAutoUpdate}
                                                       {::table-refactor/field-key      :tags
                                                        ::table-refactor/header-content "Tags"
                                                        ::table-refactor/field-cell     CellTags}])
                      :!default-columns       (r/atom [:name :description :state :created :auto-update :tags])
                      :!current-columns       (subscribe [::subs/table-current-cols])
                      :set-current-columns-fn #(dispatch [::subs/set-table-current-cols %])
                      :!data                  !resources
                      :on-row-click           #(dispatch [::routing-events/navigate routes/deployment-groups-details
                                                          {:uuid (general-utils/id->uuid (:id %))}])
                      :!enable-global-filter? (r/atom false)
                      :!enable-sorting?       (r/atom false)
                      :!enable-pagination?    (r/atom false)
                      :!pagination            !pagination}]))

(defn ops-status-overview-string [tr-fn {:keys [deployments-to-add deployments-to-remove deployments-to-update] :as _ops-status}]
  (let [deployments-sum (+ (count deployments-to-add)
                           (count deployments-to-remove)
                           (count deployments-to-update))]
    (str deployments-sum " " (tr-fn [:deployments-to-add-remove-update]))))

(defn OperationalStatus
  [{:keys [status] :as ops-status}]
  (let [tr (subscribe [::i18n-subs/tr])]
    (if (= status "OK")
      [:div {:style {:height 35}}
       [ui/Icon {:name  :circle :color (detail/ops-status->color status)
                 :style {:margin-right 5}}]
       (@tr [:everything-is-up-to-date])]
      [:div {:style {:display "flex"
                     :height  35}}
       [ui/Icon {:name  :circle :color (detail/ops-status->color status)
                 :style {:margin-right 5}}]
       [:div (ops-status-overview-string @tr ops-status)]])))

(defn DeploymentSetCard
  [{:keys [id updated name state description tags operational-status auto-update] :as _deployment-set}]
  (let [tr     (subscribe [::i18n-subs/tr])
        locale (subscribe [::i18n-subs/locale])
        href   (name->href routes/deployment-groups-details {:uuid (general-utils/id->uuid id)})]
    ^{:key id}

    [uix/Card
     {:href        href
      :header      [:div {:style {:display         :flex
                                  :justify-content :space-between
                                  :align-items     :top}}
                    (or name id)
                    [:div {:style {:text-align :right}}
                     [icons/Icon {:name (state->icon state) :style {:margin-right 0}}]
                     (when auto-update [AutoUpdateIcon])]]
      :meta        (str (str/capitalize (@tr [:updated])) " " (time/parse-ago updated @locale))
      :state       state
      :extra       [OperationalStatus operational-status]
      :description (when-not (str/blank? description) description)
      :tags        tags}]))

(defn DeploymentSetCards
  []
  (let [deployment-sets (subscribe [::subs/deployment-sets])]
    [:div style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4}
      (for [{:keys [id] :as deployment-set} (:resources @deployment-sets)]
        (when id
          ^{:key id}
          [DeploymentSetCard deployment-set]))]]))

(defn ControlBar []
  [ui/GridColumn {:width 4}
   [full-text-search-plugin/FullTextSearch
    {:db-path      [::spec/search]
     :change-event [::pagination-plugin/change-page [::spec/pagination] 1]}]])

(defn AddFirstButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Popup {:trigger (r/as-element
                          [:div
                           [uix/Button
                            {:name     ""
                             :primary  true
                             :size     :big
                             :icon     icons/i-plus-large
                             :on-click #(dispatch [::events/new-deployment-set])}]])
               :content (@tr [:add-your-first-deployment-group])}]))

(defn Main
  []
  (let [deployment-sets (subscribe [::subs/deployment-sets])]
    [:<>
     [MenuBar]
     [ui/Grid {:stackable true
               :reversed  "mobile"
               :style     {:margin-top    0
                           :margin-bottom 0}}
      [ControlBar]
      [StatisticStates true]]
     (if (zero? (:count @deployment-sets))
       [ui/Grid {:centered true}
        [AddFirstButton]]
       [:<>
        (case @view-type
          :cards [DeploymentSetCards]
          :table [DeploymentSetTable])
        [Pagination]])]))


(defn BetaBanner
  []

  [:div {:style {:padding-top 10
                 :padding-bottom 10}}
   [uix/MsgWarn {:header  [uix/TR :experimental-feature-warn]
                 :content [uix/TR :experimental-feature-warn-content-dg]}]])

(defn deployment-sets-views
  []
  (let [path-params (subscribe [::routing-subs/path-params])]
    [ui/Segment style/basic
     [BetaBanner]
     (if-let [uuid (:uuid @path-params)]
       [detail/Details uuid]
       [Main])]))
