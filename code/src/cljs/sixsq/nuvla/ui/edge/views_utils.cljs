(ns sixsq.nuvla.ui.edge.views-utils
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.edge-detail.views :as edge-detail]
    [sixsq.nuvla.ui.edge.events :as events]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.values :as utils-values]))


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
     [ui/TableCell (utils-values/format-created created)]
     [ui/TableCell [uix/Tags tags]]
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


(defn orchestrator-icon
  [orchestrator]
  [uix/Icon {:name (get utils/orchestration-icons (keyword orchestrator) "question circle")}])
