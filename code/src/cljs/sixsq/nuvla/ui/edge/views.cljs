(ns sixsq.nuvla.ui.edge.views
  (:require
    [cljs.pprint :refer [cl-format pprint]]
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.edge-detail.views :as edge-detail]
    [sixsq.nuvla.ui.edge.events :as events]
    [sixsq.nuvla.ui.edge.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(def refresh-action-id :nuvlabox-get-nuvlaboxes)


(defn refresh
  []
  (dispatch [::main-events/action-interval-start {:id        refresh-action-id
                                                  :frequency 10000
                                                  :event     [::events/get-nuvlaboxes]}]))


(defn StatisticState
  [value icon label]
  [ui/Statistic
   [ui/StatisticValue (or value "-")
    "\u2002"
    [ui/Icon {:name icon}]]
   [ui/StatisticLabel label]])


(defn StatisticStates
  []
  (let [{:keys [total new activated commissioned
                decommissioning decommissioned error]} @(subscribe [::subs/state-nuvlaboxes])]
    [ui/StatisticGroup (merge {:size "tiny"} style/center-block)
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


(defn AddButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/MenuItemWithIcon
     {:name      (@tr [:add])
      :icon-name "add"
      :on-click  #(dispatch [::events/open-modal :add])}]))


(defn MenuBar []
  (let [loading? (subscribe [::subs/loading?])]
    [ui/Menu {:borderless true}
     [AddButton]
     [main-components/RefreshMenu
      {:action-id  refresh-action-id
       :loading?   @loading?
       :on-refresh refresh}]]))


(defn CreatedNuvlaBox
  [nuvlabox-id creation-data on-close-fn tr]
  (let [nuvlabox-name-or-id (str "NuvlaBox " (or (:name creation-data)
                                                 (general-utils/id->short-uuid nuvlabox-id)))]
    [:<>
     [ui/ModalHeader
      [ui/Icon {:name "box"}] (str nuvlabox-name-or-id " created")]

     [ui/ModalContent
      [ui/CardGroup {:centered true}
       [ui/Card
        [ui/CardContent {:text-align :center}
         [ui/Header [:span {:style {:overflow-wrap "break-word"}} nuvlabox-name-or-id]]
         [ui/Icon {:name  "box"
                   :color "green"
                   :size  :massive}]]
        [ui/CopyToClipboard {:text nuvlabox-id}
         [ui/Button {:positive true
                     :icon     "clipboard"
                     :content  (@tr [:copy-nuvlabox-id])}]]]]]

     [ui/ModalActions
      [ui/Button {:on-click on-close-fn} (@tr [:close])]]]))


(defn AddModal
  []
  (let [modal-id       :add
        tr             (subscribe [::i18n-subs/tr])
        visible?       (subscribe [::subs/modal-visible? modal-id])
        user-id        (subscribe [::authn-subs/user-id])
        nuvlabox-id    (subscribe [::subs/nuvlabox-created-id])
        vpn-infra-opts (subscribe [::subs/vpn-infra-options])
        default-data   {:owner           @user-id
                       :refresh-interval 30}
        creation-data  (r/atom default-data)
        on-close-fn    #(do
                         (dispatch [::events/set-created-nuvlabox-id nil])
                         (dispatch [::events/open-modal nil])
                         (reset! creation-data default-data))
        on-add-fn      #(do
                         (dispatch [::events/create-nuvlabox
                                    (->> @creation-data
                                         (remove (fn [[_ v]] (str/blank? v)))
                                         (into {}))])
                         (reset! creation-data default-data))
        active?        (r/atom false)]
    (dispatch [::events/get-vpn-infra])
    (fn []
      [ui/Modal {:open       @visible?
                 :close-icon true
                 :on-close   on-close-fn}
       (if @nuvlabox-id
         [CreatedNuvlaBox @nuvlabox-id @creation-data on-close-fn tr]
         [:<>
          [ui/ModalHeader
           [ui/Icon {:name "add"}] (str "New NuvlaBox " (:name @creation-data))]

          [ui/ModalContent
           [ui/Table (assoc style/definition :class :nuvla-ui-editable)
            [ui/TableBody
             [uix/TableRowField (@tr [:name]), :on-change #(swap! creation-data assoc :name %)]
             [uix/TableRowField (@tr [:description]), :type :textarea,
              :on-change #(swap! creation-data assoc :description %)]
             [ui/TableRow
              [ui/TableCell {:collapsing true} "vpn"


               ]
              ^{:key (or key name)}
              [ui/TableCell
               [ui/Dropdown {:clearable   true
                             :selection   true
                             :fluid       true
                             :placeholder (@tr [:none])
                             :on-change   (ui-callback/callback
                                            :value #(swap! creation-data assoc :vpn-server-id %))
                             :options     @vpn-infra-opts}]]]
             ]]
           [ui/Accordion
            [ui/AccordionTitle {:active   @active?, :icon "dropdown", :content "Advanced"
                                :on-click #(swap! active? not)}]
            [ui/AccordionContent {:active @active?}
             [ui/Table (assoc style/definition :class :nuvla-ui-editable)
              [ui/TableBody
               [uix/TableRowField "version", :spec (s/nilable int?),
                :default-value (:version @creation-data),
                :on-change #(swap! creation-data assoc :version (general-utils/str->int %))]]]]]]

          [ui/ModalActions
           [ui/Button {:positive true
                       :on-click on-add-fn}
            (@tr [:create])]]])])))


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
    (refresh)
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
