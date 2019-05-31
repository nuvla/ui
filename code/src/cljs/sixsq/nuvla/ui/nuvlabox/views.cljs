(ns sixsq.nuvla.ui.nuvlabox.views
  (:require
    [cljs.pprint :refer [cl-format pprint]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.nuvlabox-detail.events :as nuvlabox-detail-events]
    [sixsq.nuvla.ui.nuvlabox-detail.views :as nuvlabox-detail]
    [sixsq.nuvla.ui.nuvlabox.events :as events]
    [sixsq.nuvla.ui.nuvlabox.subs :as subs]
    [sixsq.nuvla.ui.nuvlabox.utils :as utils]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn stat
  [value icon color label]

  [ui/Statistic {:size "tiny"}

   [ui/StatisticValue (or value "-")

    "\u2002"

    [ui/Icon {:name icon :color color}]]

   [ui/StatisticLabel label]])


(defn state-summary
  []
  (let [nuvlaboxes (subscribe [::subs/nuvlaboxes])
        {:keys [new activated quarantined decommissioning error]} @(subscribe [::subs/state-nuvlaboxes])
        total      (:count @nuvlaboxes)]

    [ui/Segment style/evenly
     [stat total "box" "black" "Total"]
     [stat activated "check" "black" "Activated"]
     [stat new "dolly" "black" "New"]
     [stat quarantined "eraser" "black" "Quarantined"]
     [stat decommissioning "trash" "black" "Decommissioning"]
     [stat error "exclamation" "black" "Error"]]))


(defn health-summary
  []
  (let [nuvlabox-records (subscribe [::subs/nuvlaboxes])
        health-info      (subscribe [::subs/health-info])]
    (let [{:keys [stale-count active-count]} @health-info
          total   (get @nuvlabox-records :count 0)
          unknown (max (- total stale-count active-count) 0)]
      [ui/Segment style/evenly
       [stat total "computer" "black" "NuvlaBox"]
       [stat stale-count "warning sign" "red" "stale"]
       [stat active-count "check circle" "green" "active"]
       [stat unknown "ellipsis horizontal" "yellow" "unknown"]])))


(defn filter-state []
  (let [state-selector (subscribe [::subs/state-selector])]
    ^{:key (str "state:" @state-selector)}
    [ui/Dropdown
     {:value     (or @state-selector "ALL")
      :scrolling false
      :selection true
      :options   (->>
                   utils/nuvlabox-states
                   (map (fn [state] {:value state, :text state}))
                   (cons {:value "ALL", :text "ALL"}))
      :on-change (ui-callback/value
                   #(dispatch [::events/set-state-selector (if (= % "ALL") nil %)]))}]))


(defn refresh-button
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :loading?  @loading?
        :position  "right"
        :disabled  false #_(nil? @selected-id)
        :on-click  (fn []
                     (dispatch [::events/fetch-health-info])
                     (dispatch [::events/get-nuvlaboxes]))}])))


(defn menu-bar []
  [ui/Segment style/basic
   [ui/Menu {:attached   "top"
             :borderless true}
    [refresh-button]]])


(defn health-icon
  [value]
  (case value
    true [ui/Icon {:name "check circle" :color "green"}]
    false [ui/Icon {:name "warning sign" :color "red"}]
    [ui/Icon {:name "ellipsis horizontal" :color "yellow"}]))


(defn format-nb-row
  [{:keys [id state name] :as row}]
  (let [status   (subscribe [::subs/status-nuvlabox id])
        uuid     (second (str/split id #"/"))
        on-click (fn []
                   (dispatch [::nuvlabox-detail-events/clear-detail])
                   (dispatch [::history-events/navigate (str "edge/" uuid)]))]
    [ui/TableRow #_{:on-click on-click
                  :style    {:cursor "pointer"}}
     [ui/TableCell {:collapsing true}
      [ui/Popup
       {:content @status
        :trigger (r/as-element
                   [ui/Icon {:name  "power",
                             :color (case @status
                                      :online "green"
                                      :offline "red"
                                      :unknown "yellow")}])}]]
     [ui/TableCell {:collapsing true}
      [ui/Icon {:name (utils/state->icon state)}]]
     [ui/TableCell (or name uuid)]]))


(defn nb-table
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        health-info       (subscribe [::subs/health-info])]

    (dispatch [::main-events/action-interval
               {:action    :start
                :id        :nuvlabox-get-nuvlaboxes
                :frequency 30000
                :event     [::events/get-nuvlaboxes]}])
    (fn []
      (let [{:keys [healthy?]} @health-info
            total-elements (get @nuvlaboxes :count 0)]
        [:div

         [filter-state]

         [ui/Table {:compact "very", :selectable true}
          [ui/TableHeader
           [ui/TableRow
            [ui/TableHeaderCell [ui/Icon {:name "heartbeat"}]]
            [ui/TableHeaderCell "state"]
            [ui/TableHeaderCell "name"]]]

          [ui/TableBody
           (doall
             (for [{:keys [id] :as nuvlabox} (:resources @nuvlaboxes)]
               ^{:key id}
               [format-nb-row nuvlabox]))]]

         [uix/Pagination {:totalPages   (general-utils/total-pages total-elements @elements-per-page)
                          :activePage   @page
                          :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}]]))))


(defn nb-info
  []
  (let [path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [[_ mac] @path
            n        (count @path)
            children (case n
                       1 [[menu-bar]
                          [state-summary]
                          [nb-table]]
                       2 [[nuvlabox-detail/nb-detail]]
                       [[menu-bar]
                        [health-summary]
                        [nb-table]])]
        (dispatch [::nuvlabox-detail-events/set-mac mac])
        (vec (concat [ui/Segment style/basic] children))))))


(defmethod panel/render :edge
  [path]
  [nb-info])
