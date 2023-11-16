(ns sixsq.nuvla.ui.deployment-sets.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.deployment-sets-detail.views :as detail]
            [sixsq.nuvla.ui.deployment-sets.events :as events]
            [sixsq.nuvla.ui.deployment-sets.spec :as spec]
            [sixsq.nuvla.ui.deployment-sets.subs :as subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]))

(def view-type (r/atom :cards))

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
    (str/starts-with? state "PARTIAL") icons/i-close
    :else (get {NEW     icons/i-sticky-note
                STARTED icons/i-play
                UPDATED icons/i-play
                STOPPED icons/i-stop} state)))

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
           :set-state-selector-event :sixsq.nuvla.ui.deployment-sets.events/set-state-selector
           :state-selector-subs :sixsq.nuvla.ui.deployment-sets.subs/state-selector)])]]))

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

(defn DeploymentSetRow
  [{:keys [id name description created state tags] :as _deployment-set}]
  (let [locale @(subscribe [::i18n-subs/locale])
        uuid   (general-utils/id->uuid id)]
    [ui/TableRow {:on-click #(dispatch [::routing-events/navigate routes/deployment-sets-details {:uuid uuid}])
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
     [ui/Table {:compact "very" :selectable true}
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
        href   (name->href routes/deployment-sets-details {:uuid (general-utils/id->uuid id)})]
    ^{:key id}
    [uix/Card
     {:href        href
      :header      [:<>
                    [icons/Icon {:name (state->icon state)}]
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
  (dispatch [::events/refresh])
  (let [tr (subscribe [::i18n-subs/tr])
        deployment-sets (subscribe [::subs/deployment-sets])]
    [components/LoadingPage {}
     [:<>
      [uix/PageHeader "bullseye"
       (@tr [:deployment-sets])]
      [MenuBar]
      [ui/Grid {:stackable true
                :reversed  "mobile"
                :style     {:margin-top    0
                            :margin-bottom 0}}
       [ControlBar]
       [StatisticStates true]]
      (if (empty? @deployment-sets)
        [ui/Grid {:centered true}
         [AddFirstButton]]
        [:<>
         (case @view-type
           :cards [DeploymentSetCards]
           :table [DeploymentSetTable])
         [Pagination]])]]))


(defn deployment-sets-view
  [{path :path}]
  (let [[_ path1] path
        n        (count path)
        children (case n
                   2 [detail/Details path1]
                   [Main])]
    [:<>
     [ui/Segment style/basic children]]))
