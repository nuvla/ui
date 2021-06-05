(ns sixsq.nuvla.ui.edge.views-cluster
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edge.events :as events]
    [sixsq.nuvla.ui.edge.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.edge.views-utils :as views-utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]))


(def view-type (r/atom :cards))


(defn MenuBar []
  (let [loading? (subscribe [::subs/loading?])]
    (dispatch [::events/refresh-cluster])
    (fn []
      [main-components/StickyBar
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
        [main-components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [::events/refresh-cluster])}]]])))


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
                :icon      (r/as-element [ui/Icon {:className "fas fa-chart-network"}])
                :content   (if name
                             (str name " (" cluster-id ")")
                             cluster-id)
                :subheader (str
                             nuvlabox-nodes
                             (@tr [:out-of])
                             all-nodes
                             " "
                             (if (> nuvlabox-nodes 1)
                               (str (@tr [:they-are]) " NuvlaBox " (@tr [:node]) "s")
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
     {:style  {:height 500}
      :center map/sixsq-latlng
      :zoom   3}
     (doall
       (for [{:keys [id] :as nuvlabox} (->> selected-nbs
                                            (filter #(:location %)))]
         ^{:key id}
         [NuvlaboxMapPoint nuvlabox]))]))


(defn DetailedClusterView
  []
  [ui/SegmentGroup {:stacked true
                    :color   "black"}
   [ui/Segment
    [ClusterViewHeader]]
   [ui/Segment
    (case @view-type
      :cards [NuvlaboxCards]
      :table [NuvlaboxTable]
      :map [NuvlaboxMap])]])


(defn ClusterView
  [cluster-id]
    (dispatch [::events/get-nuvlabox-cluster (str "nuvlabox-cluster/" cluster-id)])
  (let [tr (subscribe [::i18n-subs/tr])
        cluster (subscribe [::subs/nuvlabox-cluster cluster-id])]
    [:<>
     [uix/PageHeader "box" (str (general-utils/capitalize-first-letter (@tr [:edge])) " "
                                (:name @cluster))]
     [MenuBar]
     [DetailedClusterView]]))
