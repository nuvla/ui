(ns sixsq.nuvla.ui.edge.views
  (:require
    [cljs.pprint :refer [cl-format pprint]]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edge-detail.views :as edge-detail]
    [sixsq.nuvla.ui.edge.events :as events]
    [sixsq.nuvla.ui.edge.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [taoensso.timbre :as log]))


(defn StatisticState
  [value icon label]
  [ui/Statistic {:size "tiny"}
   [ui/StatisticValue (or value "-")
    "\u2002"
    [ui/Icon {:name icon}]]
   [ui/StatisticLabel label]])


(defn StatisticStates
  []
  (let [{:keys [total new activated commissioned
                decommissioning decommissioned error]} @(subscribe [::subs/state-nuvlaboxes])]
    [ui/Segment style/evenly
     [StatisticState total "box" "Total"]
     [StatisticState new (utils/state->icon utils/state-new) "New"]
     [StatisticState activated (utils/state->icon utils/state-activated) "Activated"]
     [StatisticState commissioned (utils/state->icon utils/state-commissioned) "Commissioned"]
     [StatisticState decommissioning (utils/state->icon utils/state-decommissioning) "Decommissioning"]
     [StatisticState decommissioned (utils/state->icon utils/state-decommissioned) "Decommissioned"]
     [StatisticState error (utils/state->icon utils/state-error) "Error"]]))


(defn StateFilter []
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


(defn RefreshButton
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])]
    [uix/MenuItemWithIcon
     {:name      (@tr [:refresh])
      :icon-name "refresh"
      :loading?  @loading?
      :position  "right"
      :on-click  #(dispatch [::events/get-nuvlaboxes])}]))


(defn AddButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/MenuItemWithIcon
     {:name      (@tr [:add])
      :icon-name "add"
      :on-click  #(dispatch [::events/open-modal :add])}]))


(defn MenuBar []
  [ui/Menu {:borderless true}
   [AddButton]
   [RefreshButton]])

(defn AddModal
  []
  (let [modal-id    :add
        tr          (subscribe [::i18n-subs/tr])
        visible?    (subscribe [::subs/modal-visible? modal-id])
        user-id     (subscribe [::authn-subs/user-id])
        nuvlabox-id (subscribe [::subs/nuvlabox-created-id])
        owner-id    @user-id]
    [ui/Modal {:open       @visible?
               :close-icon true
               :on-close   #(do
                              (dispatch [::events/set-created-nuvlabox-id nil])
                              (dispatch [::events/open-modal nil]))}

     [ui/ModalHeader [ui/Icon {:name "add"}] (@tr [:add])]

     [ui/ModalContent
      [ui/CardGroup {:centered true}
       [ui/Card (when-not @nuvlabox-id {:on-click #(dispatch [::events/create-nuvlabox owner-id])})
        [ui/CardContent {:text-align :center}
         [ui/Header "NuvlaBox"]
         [ui/Icon (cond-> {:name "box"
                           :size :massive}
                          @nuvlabox-id (assoc :color "green"))]]
        (when @nuvlabox-id
          [ui/CopyToClipboard {:text @nuvlabox-id}
          [ui/Button {:primary true
                      :icon    "clipboard"
                      :content "Copy your Nuvlabox ID"}]])]]]]))


(defn NuvlaboxRow
  [{:keys [id state name] :as nuvlabox}]
  (let [status   (subscribe [::subs/status-nuvlabox id])
        uuid     (general-utils/id->uuid id)
        on-click #(dispatch [::history-events/navigate (str "edge/" uuid)])]
    [ui/TableRow {:on-click on-click
                  :style    {:cursor "pointer"}}
     [ui/TableCell {:collapsing true}
      [edge-detail/StatusIcon @status]]
     [ui/TableCell {:collapsing true}
      [ui/Icon {:name (utils/state->icon state)}]]
     [ui/TableCell (or name uuid)]]))


(defn NuvlaboxTable
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]

    (dispatch [::main-events/action-interval
               {:action    :start
                :id        :nuvlabox-get-nuvlaboxes
                :frequency 30000
                :event     [::events/get-nuvlaboxes]}])
    (fn []
      (let [total-elements (get @nuvlaboxes :count 0)
            total-pages    (general-utils/total-pages total-elements @elements-per-page)]
        [:div

         [StateFilter]

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
               [NuvlaboxRow nuvlabox]))]]

         (when (> total-pages 1)
           [uix/Pagination {:totalPages   total-pages
                            :activePage   @page
                            :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}])]))))


(defmethod panel/render :edge
  [path]
  (let [[_ uuid] path
        n        (count path)
        root     [:<>
                  [MenuBar]
                  [StatisticStates]
                  [NuvlaboxTable]
                  [AddModal]]
        children (case n
                   1 root
                   2 [edge-detail/EdgeDetails uuid]
                   root)]
    [ui/Segment style/basic children]))
