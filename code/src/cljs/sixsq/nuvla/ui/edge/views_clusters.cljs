(ns sixsq.nuvla.ui.edge.views-clusters
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edge-detail.views :as edge-detail]
    [sixsq.nuvla.ui.edge.events :as events]
    [sixsq.nuvla.ui.edge.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.edge.views-utils :as views-utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(def view-type (r/atom :cards))


(defn MenuBar []
  (let [loading?      (subscribe [::subs/loading?])
        refresh-event ::events/refresh-clusters]
    (dispatch [refresh-event])
    (fn []
      [main-components/StickyBar
       [ui/Menu {:borderless true, :stackable true}
        [views-utils/AddButton]
        [ui/MenuItem {:icon     "grid layout"
                      :active   (= @view-type :cards)
                      :on-click #(reset! view-type :cards)}]
        [ui/MenuItem {:icon     "table"
                      :disabled true
                      :active   (= @view-type :table)
                      :on-click #(reset! view-type :table)}]
        [ui/MenuItem {:icon     "map"
                      :disabled true
                      :active   (= @view-type :map)
                      :on-click #(reset! view-type :map)}]
        [main-components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [refresh-event])}]]])))


(defn StatisticStates
  []
  (let [clusters (subscribe [::subs/nuvlabox-clusters])]
    (fn []
      [:div {:style {:margin     "10px auto 10px auto"
                     :text-align "center"
                     :width      "100%"}}
       [ui/StatisticGroup (merge {:widths 4 :size "tiny"} style/center-block)
        [main-components/StatisticState (:count @clusters) ["fas fa-chart-network"] "TOTAL"
         false ::events/set-state-selector ::subs/state-selector]]])))


(defn NuvlaBoxClusterCard
  [_nuvlabox-cluster nuvlaboxes]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [id cluster-id created managers workers nuvlabox-managers
                 nuvlabox-workers name description orchestrator] :as _nuvlabox-cluster}]
      (let [href          (str "edge/nuvlabox-cluster/" (general-utils/id->uuid id))
            cluster-nodes (+ (count managers) (count workers))
            nb-per-id     (group-by :id (:resources nuvlaboxes))
            name          (or name cluster-id)]
        [uix/Card
         {:on-click    #(dispatch [::history-events/navigate href])
          :href        href
          :header      [:<>
                        [ui/Icon {:className "fas fa-chart-network"}]
                        (if (> (count name) 21)
                          (str (apply str (take 20 name)) "...")
                          name)]
          :meta        [:<>
                        (str (@tr [:created]) " " (-> created time/parse-iso8601 time/ago))
                        [:br]
                        (str (str/capitalize (@tr [:orchestrator])) ": " orchestrator " ")
                        [views-utils/orchestrator-icon orchestrator]]
          :description (when-not (str/blank? description) description)
          :content     [ui/ListSA {:divided        true
                                   :vertical-align "middle"}
                        (doall
                          (for [nb-id (concat nuvlabox-managers nuvlabox-workers)]
                            (let [nuvlabox (into {} (get nb-per-id nb-id))
                                  name     (:name nuvlabox)
                                  online   (:online nuvlabox)
                                  updated  (:updated nuvlabox)]
                              ^{:key (general-utils/id->uuid nb-id)}
                              [ui/ListItem
                               [ui/Image {:avatar true}
                                [ui/Icon {:className (if (some #{nb-id} nuvlabox-managers)
                                                       "fas fa-crown"
                                                       "")}]]
                               [ui/ListContent
                                [ui/ListHeader name
                                 [:div {:style {:float "right"}}
                                  [edge-detail/OnlineStatusIcon online :corner "top right"]]]
                                [ui/ListDescription (str (@tr [:updated]) " " (-> updated time/parse-iso8601 time/ago))]]])))]
          :extra       (str (@tr [:nuvlabox-cluster-nodes]) cluster-nodes)}]))))


(defn NuvlaboxClusters
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        nuvlabox-clusters (subscribe [::subs/nuvlabox-clusters])]
    [:div style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4}
      (doall
        (for [{:keys [id] :as cluster} (:resources @nuvlabox-clusters)]
          ^{:key id}
          [NuvlaBoxClusterCard cluster @nuvlaboxes]))]]))


(defn ClustersView
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        full-text (subscribe [::subs/full-text-clusters-search])]
    [:<>
     [uix/PageHeader "fas fa-chart-network" (str (general-utils/capitalize-first-letter (@tr [:edge])) " "
                                                 (general-utils/capitalize-first-letter (@tr [:clusters])))]
     [MenuBar]
     [:div {:style {:display "flex"}}
      [main-components/SearchInput
       {:default-value @full-text
        :on-change     (ui-callback/input-callback
                         #(dispatch [::events/set-full-text-clusters-search %]))
        :style         {:display    "inline-table"
                        :margin-top "20px"}}]
      [StatisticStates]
      [ui/Input {:style {:visibility "hidden"}
                 :icon  "search"}]]
     [NuvlaboxClusters]]))
