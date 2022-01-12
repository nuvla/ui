(ns sixsq.nuvla.ui.edge.views-cluster
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.edge.events :as events]
    [sixsq.nuvla.ui.edge.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.edge.views-utils :as views-utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]))


(def view-type (r/atom :cards))


(defn MenuBar [cluster-id]
  (let [loading? (subscribe [::subs/loading?])]
    (fn []
      [components/StickyBar
       [ui/Menu {:borderless true, :stackable true}
        [views-utils/AddButton]
        [ui/MenuItem {:icon     "grid layout"
                      :active   (= @view-type :cards)
                      :on-click #(reset! view-type :cards)}]
        [ui/MenuItem {:icon     "table"
                      :active   (= @view-type :table)
                      :on-click #(reset! view-type :table)}]
        [ui/MenuItem {:icon     "map"
                      :active   (= @view-type :map)
                      :on-click #(reset! view-type :map)}]
        [components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [::events/refresh-cluster cluster-id])}]]])))


(defn ClusterViewHeader
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        nuvlabox-cluster (subscribe [::subs/nuvlabox-cluster])
        name             (:name @nuvlabox-cluster)
        cluster-id       (:cluster-id @nuvlabox-cluster)
        all-nodes        (+ (count (:managers @nuvlabox-cluster)) (count (:workers @nuvlabox-cluster)))
        nuvlabox-nodes   (+ (count (:nuvlabox-managers @nuvlabox-cluster)) (count (:nuvlabox-workers @nuvlabox-cluster)))]
    [ui/Header {:as        "h3"
                :float     "left"
                :icon      (r/as-element [uix/Icon {:name "fas fa-chart-network"}])
                :content   (if name
                             (str name " (" cluster-id ")")
                             cluster-id)
                :subheader (str
                             nuvlabox-nodes
                             (@tr [:out-of])
                             all-nodes
                             " "
                             (if (> nuvlabox-nodes 1)
                               (str (@tr [:they-are]) " NuvlaBox " (@tr [:nodes]))
                               (str (@tr [:it-is-a]) " NuvlaBox " (@tr [:node]))))}]))


(defn NuvlaboxCards
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        nuvlabox-clusters (subscribe [::subs/nuvlabox-clusters])
        managers          (distinct
                            (apply concat
                                   (map :nuvlabox-managers (:resources @nuvlabox-clusters))))
        current-cluster   (subscribe [::subs/nuvlabox-cluster])
        selected-nbs      (if @current-cluster
                            (for [target-nb-id (concat (:nuvlabox-managers @current-cluster)
                                                       (:nuvlabox-workers @current-cluster))]
                              (into {} (get (group-by :id (:resources @nuvlaboxes)) target-nb-id)))
                            (:resources @nuvlaboxes))]
    [:div style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4}
      (for [{:keys [id] :as nuvlabox} selected-nbs]
        (when id
          ^{:key id}
          [views-utils/NuvlaboxCard nuvlabox managers]))]]))


(defn NuvlaboxTable
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        nuvlabox-clusters (subscribe [::subs/nuvlabox-clusters])
        managers          (distinct
                            (apply concat
                                   (map :nuvlabox-managers (:resources @nuvlabox-clusters))))
        current-cluster   (subscribe [::subs/nuvlabox-cluster])
        selected-nbs      (if @current-cluster
                            (for [target-nb-id (concat (:nuvlabox-managers @current-cluster)
                                                       (:nuvlabox-workers @current-cluster))]
                              (into {} (get (group-by :id (:resources @nuvlaboxes)) target-nb-id)))
                            (:resources @nuvlaboxes))]
    [:div style/center-items
     [ui/Table {:compact "very", :selectable true}
      [ui/TableHeader
       [ui/TableRow
        [ui/TableHeaderCell [ui/Icon {:name "heartbeat"}]]
        [ui/TableHeaderCell "state"]
        [ui/TableHeaderCell "name"]
        [ui/TableHeaderCell "description"]
        [ui/TableHeaderCell "created"]
        [ui/TableHeaderCell "tags"]
        [ui/TableHeaderCell "manager"]]]

      [ui/TableBody
       (for [{:keys [id] :as nuvlabox} selected-nbs]
         (when id
           ^{:key id}
           [views-utils/NuvlaboxRow nuvlabox managers]))]]]))


(defn NuvlaboxMapPoint
  [{:keys [id name location online]}]
  (let [uuid     (general-utils/id->uuid id)
        on-click #(dispatch [::history-events/navigate (str "edge/" uuid)])]
    [map/CircleMarker {:on-click on-click
                       :center   (map/longlat->latlong location)
                       :color    (utils/map-online->color online)
                       :opacity  0.5
                       :weight   2}
     [map/Tooltip (or name id)]]))


(defn NuvlaboxMap
  []
  (let [nuvlaboxes      (subscribe [::subs/nuvlaboxes])
        current-cluster (subscribe [::subs/nuvlabox-cluster])
        selected-nbs    (if @current-cluster
                          (for [target-nb-id (concat (:nuvlabox-managers @current-cluster)
                                                     (:nuvlabox-workers @current-cluster))]
                            (into {} (get (group-by :id (:resources @nuvlaboxes)) target-nb-id)))
                          (:resources @nuvlaboxes))]
    [map/MapBox
     {}
     (doall
       (for [{:keys [id] :as nuvlabox} (->> selected-nbs
                                            (filter #(:location %)))]
         ^{:key id}
         [NuvlaboxMapPoint nuvlabox]))]))


(defn DetailedClusterView
  []
  (let [tr      (subscribe [::i18n-subs/tr])
        cluster (subscribe [::subs/nuvlabox-cluster])
        {:keys [id name description owners tags created updated version orchestrator]} @cluster]
    [:<>
     [ui/Segment {:secondary true
                  :color     "blue"
                  :raised    true}
      [:h4 (str/capitalize (@tr [:cluster])) " " (@tr [:summary])]
      [ClusterViewHeader]


      [ui/Table {:basic "very" :style {:display "inline", :floated "left"}}
       [ui/TableBody
        [ui/TableRow
         [ui/TableCell (str/capitalize (str (@tr [:name])))]
         [ui/TableCell name]]
        [ui/TableRow
         [ui/TableCell (str/capitalize (str (@tr [:description])))]
         [ui/TableCell description]]
        [ui/TableRow
         [ui/TableCell "Id"]
         [ui/TableCell id]]
        [ui/TableRow
         [ui/TableCell (str/capitalize (str (@tr [:created])))]
         [ui/TableCell (-> created time/parse-iso8601 time/ago)]]
        [ui/TableRow
         [ui/TableCell (str/capitalize (str (@tr [:updated])))]
         [ui/TableCell (-> updated time/parse-iso8601 time/ago)]]
        [ui/TableRow
         [ui/TableCell (str/capitalize (str (@tr [:version])))]
         [ui/TableCell version]]
        [ui/TableRow
         [ui/TableCell (str/capitalize (@tr [:orchestrator]))]
         [ui/TableCell
          orchestrator " "
          [views-utils/orchestrator-icon orchestrator]]]
        (when (not-empty owners)
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:owner]))]
           [ui/TableCell (str/join ", " owners)]])
        (when tags
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:tags]))]
           [ui/TableCell
            [uix/Tags tags]]])]]]
     [ui/Segment
      (case @view-type
        :cards [NuvlaboxCards]
        :table [NuvlaboxTable]
        :map [NuvlaboxMap])]]))


(defn TabOverview
  []
  (let [device (subscribe [::main-subs/device])]
    (fn []
      [ui/TabPane
       [ui/Grid {:columns   (if (contains? #{:wide-screen} @device) 2 1)
                 :stackable true
                 :centered  true
                 :padded    true}
        [ui/GridRow
         [ui/GridColumn {:stretched true}
          [DetailedClusterView]]]]])))


(defn tabs
  []
  (let [cluster   (subscribe [::subs/nuvlabox-cluster])
        can-edit? (subscribe [::subs/can-edit-cluster?])]
    [{:menuItem {:content "Overview"
                 :key     "overview"
                 :icon    "info"}
      :render   (fn [] (r/as-element [TabOverview]))}
     (acl/TabAcls cluster @can-edit? ::events/edit)]))


(defn TabsCluster
  []
  (fn []
    (let [active-index (subscribe [::subs/active-tab-index])]
      [ui/Tab
       {:menu        {:secondary true
                      :pointing  true
                      :style     {:display        "flex"
                                  :flex-direction "row"
                                  :flex-wrap      "wrap"}}
        :panes       (tabs)
        :activeIndex @active-index
        :onTabChange (fn [_ data]
                       (let [active-index (. data -activeIndex)]
                         (dispatch [::events/set-active-tab-index active-index])))}])))

(defn ClusterView
  [cluster-id]
  (let [tr      (subscribe [::i18n-subs/tr])
        cluster (subscribe [::subs/nuvlabox-cluster cluster-id])]
    (dispatch [::events/refresh-cluster cluster-id])
    (fn [cluster-id]
      [components/LoadingPage {:dimmable? true}
       [:<>
        [components/NotFoundPortal
         ::subs/nuvlabox-not-found?
         :no-nuvlabox-cluster-message-header
         :no-nuvlabox-cluster-message-content]
        [uix/PageHeader "fas fa-chart-network" (str (general-utils/capitalize-first-letter (@tr [:edge])) " "
                                                    (:name @cluster))]
        [MenuBar cluster-id]
        [TabsCluster]]])))
