(ns sixsq.nuvla.ui.deployment-sets.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.deployment-sets-detail.views :as detail]
            [sixsq.nuvla.ui.deployment-sets.events :as events]
            [sixsq.nuvla.ui.deployment-sets.spec :as spec]
            [sixsq.nuvla.ui.deployment-sets.subs :as subs]
            [sixsq.nuvla.ui.history.events :as history-events]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.panel :as panel]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]))

(def view-type (r/atom :cards))

(def ^:const STARTED "STARTED")
(def ^:const CREATED "CREATED")
(def ^:const STOPPED "STOPPED")
(def ^:const PENDING "PENDING")

(defn state->icon
  [state]
  (if (str/ends-with? state "ING")
    "sync"
    (get {STARTED "play"
          STOPPED "stop"
          CREATED "circle outline"} state)))

(defn StatisticStates
  [clickable?]
  (let [summary  (subscribe [::subs/deployment-sets-summary])
        terms    (general-utils/aggregate-to-map
                   (get-in @summary [:aggregations :terms:state :buckets]))
        started  (:STARTED terms 0)
        starting (:STARTING terms 0)
        creating (:CREATING terms 0)
        created  (:CREATED terms 0)
        stopping (:STOPPING terms 0)
        stopped  (:STOPPED terms 0)
        pending  (+ starting creating stopping)
        total    (:count @summary)]
    [ui/GridColumn {:width 8}
     [ui/StatisticGroup {:size  "tiny"
                         :style {:justify-content "center"
                                 :padding-top     "20px"
                                 :padding-bottom  "20px"}}
      [components/StatisticState {:value total,
                                  :icons ["fal fa-bullseye"],
                                  :label "TOTAL",
                                  :clickable? clickable?,
                                  :positive-color nil,
                                  :set-state-selector-event :sixsq.nuvla.ui.deployment-sets.events/set-state-selector,
                                  :state-selector-subs :sixsq.nuvla.ui.deployment-sets.subs/state-selector}]
      [components/StatisticState {:value created,
                                  :icons [(str "fal " (state->icon CREATED))],
                                  :label CREATED,
                                  :clickable? clickable?,
                                  :positive-color "blue",
                                  :set-state-selector-event :sixsq.nuvla.ui.deployment-sets.events/set-state-selector,
                                  :state-selector-subs :sixsq.nuvla.ui.deployment-sets.subs/state-selector}]
      [components/StatisticState {:value started,
                                  :icons [(str "fal " (state->icon STARTED))],
                                  :label STARTED,
                                  :clickable? clickable?,
                                  :positive-color "green",
                                  :set-state-selector-event :sixsq.nuvla.ui.deployment-sets.events/set-state-selector,
                                  :state-selector-subs :sixsq.nuvla.ui.deployment-sets.subs/state-selector}]
      [components/StatisticState {:value stopped,
                                  :icons [(str "fal " (state->icon STOPPED))],
                                  :label STOPPED,
                                  :clickable? clickable?,
                                  :positive-color "red",
                                  :set-state-selector-event :sixsq.nuvla.ui.deployment-sets.events/set-state-selector,
                                  :state-selector-subs :sixsq.nuvla.ui.deployment-sets.subs/state-selector}]
      [components/StatisticState {:value pending,
                                  :icons [(str "fal " (state->icon PENDING))],
                                  :label PENDING,
                                  :clickable? clickable?,
                                  :positive-color "brown",
                                  :set-state-selector-event :sixsq.nuvla.ui.deployment-sets.events/set-state-selector,
                                  :state-selector-subs :sixsq.nuvla.ui.deployment-sets.subs/state-selector}]]]))


(defn AddButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/MenuItem
     {:name     (@tr [:add])
      :icon     "add"
      :on-click #(dispatch
                   [::history-events/navigate "deployment-sets/New"])}]))

(defn MenuBar []
  (let [loading? (subscribe [::subs/loading?])]
    (fn []
      [components/StickyBar
       [ui/Menu {:borderless true, :stackable true}
        [AddButton]
        [ui/MenuItem {:icon     "grid layout"
                      :active   (= @view-type :cards)
                      :on-click #(reset! view-type :cards)}]
        [ui/MenuItem {:icon     "table"
                      :active   (= @view-type :table)
                      :on-click #(reset! view-type :table)}]
        [components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [::events/refresh])}]]])))

(defn DeploymentSetRow
  [{:keys [id name description created state tags] :as _deployment-set}]
  (let [locale @(subscribe [::i18n-subs/locale])
        uuid   (general-utils/id->uuid id)]
    [ui/TableRow {:on-click #(dispatch [::history-events/navigate (str "deployment-sets/" uuid)])
                  :style    {:cursor "pointer"}}
     [ui/TableCell (or name uuid)]
     [ui/TableCell description]
     [ui/TableCell state]
     [ui/TableCell (time/parse-ago created locale)]
     [ui/TableCell [uix/Tags tags]]]))

(defn Pagination
  []
  (let [deployment-sets @(subscribe [::subs/deployment-sets])]
    [pagination-plugin/Pagination {:db-path      [::spec/pagination]
                                   :total-items  (get deployment-sets :count 0)
                                   :change-event [::events/refresh]}]))

(defn DeploymentSetTable
  []
  (let [deployment-sets (subscribe [::subs/deployment-sets])]
    [:div style/center-items
     [ui/Table {:compact "very", :selectable true}
      [ui/TableHeader
       [ui/TableRow
        [ui/TableHeaderCell "name"]
        [ui/TableHeaderCell "description"]
        [ui/TableHeaderCell "state"]
        [ui/TableHeaderCell "created"]
        [ui/TableHeaderCell "tags"]]]

      [ui/TableBody
       (for [{:keys [id] :as deployment-set} (:resources @deployment-sets)]
         (when id
           ^{:key id}
           [DeploymentSetRow deployment-set]))]]]))

(defn DeploymentSetCard
  [{:keys [id created name state description tags] :as _deployment-set}]
  (let [tr     (subscribe [::i18n-subs/tr])
        locale (subscribe [::i18n-subs/locale])
        href   (str "deployment-sets/" (general-utils/id->uuid id))]
    ^{:key id}
    [uix/Card
     {:on-click    #(dispatch [::history-events/navigate href])
      :href        href
      :header      [:<>
                    [ui/Icon {:name (state->icon state)}]
                    (or name id)]
      :meta        (str (@tr [:created]) " " (time/parse-ago created @locale))
      :state       state
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

(defn Main
  []
  (dispatch [::events/refresh])
  (let [tr (subscribe [::i18n-subs/tr])]
    [components/LoadingPage {}
     [:<>
      [uix/PageHeader "bullseye"
       (@tr [:deployment-sets])]
      [MenuBar]
      [ui/Grid {:columns   3
                :stackable true
                :reversed  "mobile"}
       [ControlBar]
       [StatisticStates true]]
      (case @view-type
        :cards [DeploymentSetCards]
        :table [DeploymentSetTable])
      [Pagination]]]))


(defmethod panel/render :deployment-sets
  [path]
  (let [[_ path1] path
        n        (count path)
        children (case n
                   2 [detail/Details path1]
                   [Main])]
    [:<>
     [ui/Segment style/basic children]]))
