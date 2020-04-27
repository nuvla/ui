(ns sixsq.nuvla.ui.edge.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edge-detail.views :as edge-detail]
    [sixsq.nuvla.ui.edge.events :as events]
    [sixsq.nuvla.ui.edge.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.zip :as zip]))


(def view-type (r/atom :cards))

(defn StatisticState
  [value icon label]
  (let [state-selector (subscribe [::subs/state-selector])
        selected?      (or
                         (= label @state-selector)
                         (and (= label "TOTAL")
                              (= @state-selector nil)))
        color          (if selected? "black" "grey")]
    [ui/Statistic {:style    {:cursor "pointer"}
                   :color    color
                   :on-click #(dispatch [::events/set-state-selector
                                         (if (= label "TOTAL") nil label)])}
     [ui/StatisticValue (or value "-")
      "\u2002"
      [ui/Icon {:size (when selected? "large") :name icon}]]
     [ui/StatisticLabel label]]))


(defn StatisticStates
  []
  (let [{:keys [total new activated commissioned
                decommissioning decommissioned error]} @(subscribe [::subs/state-nuvlaboxes])]
    [ui/StatisticGroup (merge {:size "tiny"} style/center-block)
     [StatisticState total "box" "TOTAL"]
     [StatisticState new (utils/state->icon utils/state-new) "NEW"]
     [StatisticState activated (utils/state->icon utils/state-activated) "ACTIVATED"]
     [StatisticState commissioned (utils/state->icon utils/state-commissioned) "COMMISSIONED"]
     [StatisticState decommissioning
      (utils/state->icon utils/state-decommissioning) "DECOMMISSIONING"]
     [StatisticState decommissioned (utils/state->icon utils/state-decommissioned) "DECOMMISSIONED"]
     [StatisticState error (utils/state->icon utils/state-error) "ERROR"]]))


(defn AddButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/MenuItemWithIcon
     {:name      (@tr [:add])
      :icon-name "add"
      :on-click  #(dispatch [::events/open-modal :add])}]))


(defn MenuBar []
  (let [loading?  (subscribe [::subs/loading?])
        full-text (subscribe [::subs/full-text-search])]
    (dispatch [::events/refresh])
    (fn []
      [:<>
       [ui/Menu {:borderless true, :stackable true}
        [AddButton]
        [ui/MenuItem {:icon     "grid layout"
                      :active   (= @view-type :cards)
                      :on-click #(reset! view-type :cards)}]
        [ui/MenuItem {:icon     "table"
                      :active   (= @view-type :table)
                      :on-click #(reset! view-type :table)}]
        [ui/MenuItem {:icon     "map"
                      :active   (= @view-type :map)
                      :on-click #(reset! view-type :map)}]
        [main-components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [::events/refresh])}]]
       [main-components/SearchInput
        {:default-value @full-text
         :on-change     (ui-callback/input-callback
                          #(dispatch [::events/set-full-text-search %]))}]])))


(defn CreatedNuvlaBox
  [nuvlabox-id creation-data nuvlabox-release-data on-close-fn tr]
  (let [nuvlabox-release     (:nb-selected nuvlabox-release-data)
        nuvlabox-peripherals (:nb-assets nuvlabox-release-data)
        zip-url              (r/atom nil)
        download-files       (utils/prepare-compose-files
                               nuvlabox-release nuvlabox-peripherals nuvlabox-id)]
    (zip/create download-files #(reset! zip-url %))
    (fn []
      (let [nuvlabox-name-or-id (str "NuvlaBox " (or (:name creation-data)
                                                     (general-utils/id->short-uuid nuvlabox-id)))
            execute-command     (str "docker-compose -p nuvlabox -f "
                                     (str/join " -f " (map :name download-files)) " up -d")]
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

         [ui/Divider {:horizontal true}
          [ui/Header "Quick Installation"]]

         [ui/Segment {:loading    (nil? @zip-url)
                      :text-align :center
                      :raised     true}
          [ui/Label {:circular true
                     :color    "green"} "1"]
          [:h5 {:style {:margin "0.5em 0 1em 0"}}
           "Download the compose file(s)"]
          [:a {:href     @zip-url
               :target   "_blank"
               :style    {:margin "1em"}
               :download "nuvlabox-engine.zip"} "nuvlabox-engine.zip"]]

         [ui/Segment {:text-align :center
                      :raised     true}
          [ui/Label {:circular true
                     :color    "green"} "2"]
          [:h5 {:style {:margin "0.5em 0 1em 0"}}
           "Unzip & Execute "
           [ui/CopyToClipboard {:text execute-command}
            [:a {:href  "#"
                 :style {:font-size   "0.9em"
                         :color       "grey"
                         :font-style  "italic"
                         :font-weight "lighter"}} "(click to copy)"]]]
          [:span {:style {:font "1em Inconsolata, monospace"}} execute-command]]

         [ui/Container {:text-align :center
                        :style      {:margin "0.2em"}}
          [:span "Full documentation at "
           [:a {:href   "https://docs.nuvla.io/docs/nuvlabox/nuvlabox-engine/quickstart.html"
                :target "_blank"} "Nuvla Docs"]]]

         [ui/ModalActions
          [ui/Button {:on-click on-close-fn} (@tr [:close])]]]))))


(defn AddModal
  []
  (let [modal-id              :add
        tr                    (subscribe [::i18n-subs/tr])
        visible?              (subscribe [::subs/modal-visible? modal-id])
        nuvlabox-id           (subscribe [::subs/nuvlabox-created-id])
        vpn-infra-opts        (subscribe [::subs/vpn-infra-options])
        nb-releases           (subscribe [::subs/nuvlabox-releases])
        nb-releases-options   (map
                                (fn [{:keys [release]}]
                                  {:key release, :text release, :value release})
                                @nb-releases)
        nb-releases-by-rel    (group-by :release @nb-releases)
        default-data          {:refresh-interval 30}
        first-nb-release      (first @nb-releases)
        creation-data         (r/atom default-data)
        default-release-data  {:nb-rel      (:release first-nb-release)
                               :nb-selected first-nb-release
                               :nb-assets   (->> first-nb-release
                                                 :compose-files
                                                 (map :scope)
                                                 set)}
        nuvlabox-release-data (r/atom default-release-data)
        advanced?             (r/atom false)
        on-close-fn           #(do
                                 (dispatch [::events/set-created-nuvlabox-id nil])
                                 (dispatch [::events/open-modal nil])
                                 (reset! advanced? false)
                                 (reset! creation-data default-data)
                                 (reset! nuvlabox-release-data default-release-data))
        on-add-fn             #(do
                                 (dispatch [::events/create-nuvlabox
                                            (->> @creation-data
                                                 (remove (fn [[_ v]] (str/blank? v)))
                                                 (into {}))])
                                 (reset! creation-data default-data))]
    (fn []
      (when (= (count @vpn-infra-opts) 1)
        (swap! creation-data assoc :vpn-server-id (-> @vpn-infra-opts first :value)))
      [ui/Modal {:open       @visible?
                 :close-icon true
                 :on-close   on-close-fn}
       (if @nuvlabox-id
         [CreatedNuvlaBox @nuvlabox-id @creation-data @nuvlabox-release-data on-close-fn tr]
         [:<>
          [ui/ModalHeader
           [ui/Icon {:name "add"}] (str "New NuvlaBox " (:name @creation-data))]

          [ui/ModalContent

           [ui/Table style/definition
            [ui/TableBody
             [uix/TableRowField (@tr [:name]), :on-change #(swap! creation-data assoc :name %),
              :default-value (:name @creation-data)]
             [uix/TableRowField (@tr [:description]), :type :textarea,
              :on-change #(swap! creation-data assoc :description %)
              :default-value (:name @creation-data)]
             [ui/TableRow
              [ui/TableCell {:collapsing true} "vpn"]
              ^{:key (or key name)}
              [ui/TableCell
               [ui/Dropdown {:clearable   (> (count @vpn-infra-opts) 1)
                             :selection   true
                             :fluid       true
                             :placeholder (@tr [:none])
                             :value       (:vpn-server-id @creation-data)
                             :on-change   (ui-callback/callback
                                            :value #(swap! creation-data assoc :vpn-server-id %))
                             :options     @vpn-infra-opts}]]]]]
           [ui/Accordion
            [ui/AccordionTitle {:active   @advanced?, :icon "dropdown", :content "Advanced"
                                :on-click #(swap! advanced? not)}]
            [ui/AccordionContent {:active @advanced?}

             (let [{nb-rel                                  :nb-rel
                    nb-assets                               :nb-assets
                    {:keys [compose-files url pre-release]} :nb-selected} @nuvlabox-release-data]
               [ui/Segment
                [:h3 {:style {:font-variant "small-caps"}} "version"]
                [ui/Dropdown {:selection   true
                              :placeholder nb-rel
                              :value       nb-rel
                              :options     nb-releases-options
                              :on-change   (ui-callback/value
                                             (fn [value]
                                               (swap! nuvlabox-release-data
                                                      assoc :nb-rel value)
                                               (swap! creation-data assoc
                                                      :version (-> value
                                                                   utils/get-major-version
                                                                   general-utils/str->int))
                                               (swap! nuvlabox-release-data assoc
                                                      :nb-selected
                                                      (->> value
                                                           (get nb-releases-by-rel)
                                                           (into (sorted-map))))
                                               (swap! nuvlabox-release-data assoc :nb-assets
                                                      (set (map :scope compose-files)))))}]
                [:a {:href   url
                     :target "_blank"
                     :style  {:margin "1em"}
                     } "Release notes"]
                (when pre-release
                  [ui/Popup
                   {:trigger        (r/as-element [ui/Icon {:name "exclamation triangle"}])
                    :content        (str "This version is a pre-release, "
                                         "and thus not meant for production!")
                    :on             "hover"
                    :hide-on-scroll true}])
                [ui/Container
                 (when (> (count compose-files) 1)
                   [ui/Popup
                    {:trigger        (r/as-element [:span "Additional modules: "])
                     :content        (str "This release lets you choose optional modules for "
                                          "automatic peripheral discovery")
                     :on             "hover"
                     :hide-on-scroll true}])
                 (doall
                   (for [{:keys [scope]} compose-files]
                     (when-not (#{"core" ""} scope)
                       [ui/Checkbox {:key             scope
                                     :label           scope
                                     :default-checked (contains? (:nb-assets @nuvlabox-release-data)
                                                                 scope)
                                     :style           {:margin "1em"}
                                     :on-change       (ui-callback/checked
                                                        (fn [checked]
                                                          (if checked
                                                            (swap! nuvlabox-release-data assoc
                                                                   :nb-assets
                                                                   (conj nb-assets scope))
                                                            (swap! nuvlabox-release-data assoc
                                                                   :nb-assets
                                                                   (-> @nuvlabox-release-data
                                                                       :nb-assets
                                                                       (disj scope))))))}])))]])]]]

          [ui/ModalActions
           [ui/Button {:positive true
                       :on-click on-add-fn}
            (@tr [:create])]]])])))


(defn AddModalWrapper
  []
  (let [nb-release (subscribe [::subs/nuvlabox-releases])]
    ^{:key (count @nb-release)}
    [AddModal]))

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


(defn Pagination
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        total-elements    (get @nuvlaboxes :count 0)
        total-pages       (general-utils/total-pages total-elements @elements-per-page)]

    [uix/Pagination {:totalPages   total-pages
                     :activePage   @page
                     :onPageChange (ui-callback/callback
                                     :activePage #(dispatch [::events/set-page %]))}]))


(defn NuvlaboxTable
  []
  (let [nuvlaboxes (subscribe [::subs/nuvlaboxes])]
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
          [NuvlaboxRow nuvlabox]))]]))


(defn NuvlaboxMapPoint
  [{:keys [id name location] :as nuvlabox}]
  (let [status   (subscribe [::subs/status-nuvlabox id])
        uuid     (general-utils/id->uuid id)
        on-click #(dispatch [::history-events/navigate (str "edge/" uuid)])]
    [map/CircleMarker {:on-click on-click
                       :center   (map/longlat->latlong location)
                       :color    (utils/map-status->color @status)
                       :opacity  0.5
                       :weight   2}
     [map/Tooltip (or name id)]]))


(defn NuvlaboxCards
  []
  (let [nuvlaboxes (subscribe [::subs/nuvlaboxes])]
    [ui/CardGroup {:centered true}
     (doall
       (for [{:keys [id] :as nuvlabox} (:resources @nuvlaboxes)]
         (let [status      (subscribe [::subs/status-nuvlabox id])
               uuid        (general-utils/id->uuid id)
               on-click-fn #(dispatch [::history-events/navigate (str "edge/" uuid)])]
           ^{:key id}
           [edge-detail/NuvlaboxCard nuvlabox @status :on-click on-click-fn])))]))


(defn NuvlaboxMap
  []
  (let [nuvlaboxes (subscribe [::subs/nuvlaboxes])]
    [map/MapBox
     {:style  {:height 500}
      :center map/sixsq-latlng
      :zoom   3}
     (doall
       (for [{:keys [id] :as nuvlabox} (->> @nuvlaboxes
                                            :resources
                                            (filter #(:location %)))]
         ^{:key id}
         [NuvlaboxMapPoint nuvlabox]))]))


(defmethod panel/render :edge
  [path]
  (let [[_ uuid] path
        n        (count path)
        root     [:<>
                  [MenuBar]
                  [StatisticStates]
                  (case @view-type
                    :cards [NuvlaboxCards]
                    :table [NuvlaboxTable]
                    :map [NuvlaboxMap])
                  (when-not (= @view-type :map)
                    [Pagination])
                  [AddModalWrapper]]
        children (case n
                   1 root
                   2 [edge-detail/EdgeDetails uuid]
                   root)]
    (dispatch [::events/get-vpn-infra])
    (dispatch [::events/get-nuvlabox-releases])
    [ui/Segment style/basic children]))
