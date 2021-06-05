(ns sixsq.nuvla.ui.edge.views-utils
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.edge-detail.views :as edge-detail]
    [sixsq.nuvla.ui.edge.events :as events]
    [sixsq.nuvla.ui.edge.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]))


(defn FormatTags
  [tags id]
  [ui/LabelGroup {:size  "tiny"
                  :color "teal"
                  :style {:margin-top 10, :max-height 150, :overflow "auto"}}
   (for [tag tags]
     ^{:key (str id "-" tag)}
     [ui/Label {:style {:max-width     "15ch"
                        :overflow      "hidden"
                        :text-overflow "ellipsis"
                        :white-space   "nowrap"}}
      [ui/Icon {:name "tag"}] tag])])


(defn NuvlaboxRow
  [{:keys [id name description created state tags online] :as _nuvlabox} managers]
  (let [uuid (general-utils/id->uuid id)]
    [ui/TableRow {:on-click #(dispatch [::history-events/navigate (str "edge/" uuid)])
                  :style    {:cursor "pointer"}}
     [ui/TableCell {:collapsing true}
      [edge-detail/OnlineStatusIcon online]]
     [ui/TableCell {:collapsing true}
      [ui/Icon {:icon (utils/state->icon state)}]]
     [ui/TableCell (or name uuid)]
     [ui/TableCell description]
     [ui/TableCell (utils/format-created created)]
     [ui/TableCell [FormatTags tags id]]
     [ui/TableCell {:collapsing true}
      (when (some #{id} managers)
        [ui/Icon {:name "check"}])]]))


(defn AddButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/MenuItem
     {:name     (@tr [:add])
      :icon     "add"
      :on-click #(dispatch
                   [::main-events/subscription-required-dispatch
                    [::events/open-modal :add]])}]))


(defn NuvlaboxCard
  [_nuvlabox _managers]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [id name description created state tags online] :as _nuvlabox} managers]
      (let [href (str "edge/" (general-utils/id->uuid id))]
        ^{:key id}
        [uix/Card
         {:on-click    #(dispatch [::history-events/navigate href])
          :href        href
          :header      [:<>
                        [:div {:style {:float "right"}}
                         [edge-detail/OnlineStatusIcon online :corner "top right"]]
                        [ui/IconGroup
                         [ui/Icon {:name "box"}]
                         (when (some #{id} managers)
                           [ui/Icon {:className "fas fa-crown"
                                     :corner    true
                                     :color     "blue"}])]
                        (or name id)]
          :meta        (str (@tr [:created]) " " (-> created time/parse-iso8601 time/ago))
          :state       state
          :description (when-not (str/blank? description) description)
          :tags        tags}]))))


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
           [NuvlaboxRow nuvlabox managers]))]]]))


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
  (let [nuvlaboxes   (subscribe [::subs/nuvlaboxes])
        selected-nbs (:resources @nuvlaboxes)]
    [map/MapBox
     {:style  {:height 500}
      :center map/sixsq-latlng
      :zoom   3}
     (doall
       (for [{:keys [id] :as nuvlabox} (->> selected-nbs
                                            (filter #(:location %)))]
         ^{:key id}
         [NuvlaboxMapPoint nuvlabox]))]))
