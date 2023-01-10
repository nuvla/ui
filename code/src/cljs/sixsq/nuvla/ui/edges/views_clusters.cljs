(ns sixsq.nuvla.ui.edges.views-clusters
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.edges.events :as events]
            [sixsq.nuvla.ui.edges.subs :as subs]
            [sixsq.nuvla.ui.edges.views-utils :as views-utils]
            [sixsq.nuvla.ui.history.events :as history-events]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.view-components :refer [OnlineStatusIcon]]))


(defn StatisticStates
  []
  (let [clusters (subscribe [::subs/nuvlabox-clusters])]
    (fn []
      [ui/StatisticGroup {:widths 4 :size "tiny"}
       [components/StatisticState  {:value (:count @clusters),
                                    :icons ["fas fa-chart-network"],
                                    :label "TOTAL",
                                    :clickable false,
                                    :positive-color "",
                                    :set-state-selector-event :sixsq.nuvla.ui.edges.events/set-state-selector,
                                    :state-selector-subs :sixsq.nuvla.ui.edges.subs/state-selector}]])))


(defn NuvlaBoxClusterCard
  [_nuvlabox-cluster nuvlaboxes]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [id cluster-id created managers workers nuvlabox-managers
                 nuvlabox-workers name description orchestrator status-notes] :as _nuvlabox-cluster}]
      (let [href          (str "edges/nuvlabox-cluster/" (general-utils/id->uuid id))
            cluster-nodes (+ (count managers) (count workers))
            nb-per-id     (group-by :id (:resources @nuvlaboxes))
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
                                  [OnlineStatusIcon online :corner "top right"]]]
                                [ui/ListDescription (str (@tr [:updated]) " " (-> updated time/parse-iso8601 time/ago))]]])))]
          :extra       [:<>
                        (when (not-empty status-notes)
                          [:div {:style {:float "right"}}
                           [ui/Popup {:content        (r/as-element [ui/MessageList {:items status-notes}])
                                      :position       "bottom center"
                                      :hide-on-scroll true
                                      :hoverable      true
                                      :trigger        (r/as-element [ui/Icon {:name  "info circle"
                                                                              :color "brown"}])}]])
                        (str (@tr [:nuvlabox-cluster-nodes]) cluster-nodes)]}]))))


(defn NuvlaboxClusters
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes-in-clusters])
        nuvlabox-clusters (subscribe [::subs/nuvlabox-clusters])]
    [:div style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4}
      (doall
        (for [{:keys [id] :as cluster} (:resources @nuvlabox-clusters)]
          ^{:key id}
          [NuvlaBoxClusterCard cluster nuvlaboxes]))]]))
