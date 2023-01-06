(ns sixsq.nuvla.ui.edges.views-utils
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edges.events :as events]
    [sixsq.nuvla.ui.edges.subs :as subs]
    [sixsq.nuvla.ui.edges.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.routing.utils :refer [name->href]]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.view-components :refer [OnlineStatusIcon]]))


(defn NuvlaboxRow
  [{:keys [id name description created state tags online] :as _nuvlabox} managers]
  (let [locale (subscribe [::i18n-subs/locale])
        uuid   (general-utils/id->uuid id)]
    [ui/TableRow {:on-click #(dispatch [::history-events/navigate (str "edges/" uuid)])
                  :style    {:cursor "pointer"}}
     [ui/TableCell {:collapsing true}
      [OnlineStatusIcon online]]
     [ui/TableCell {:collapsing true}
      [ui/Icon {:icon (utils/state->icon state)}]]
     [ui/TableCell (or name uuid)]
     [ui/TableCell description]
     [ui/TableCell (time/parse-ago created @locale)]
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

(defn- date-string->time-ago [created]
  (-> created time/parse-iso8601 time/ago))


(defn NuvlaboxCard
  [_nuvlabox _managers]
  (let [tr     (subscribe [::i18n-subs/tr])
        locale (subscribe [::i18n-subs/locale])]
    (fn [{:keys [id name description created state tags online refresh-interval created-by] :as _nuvlabox} managers]
      (let [href                  (name->href :edges-details {:uuid  (general-utils/id->uuid id)})
            next-heartbeat-moment @(subscribe [::subs/next-heartbeat-moment id])
            creator               (subscribe [::session-subs/resolve-user created-by])]
        ^{:key id}
        [uix/Card
         {:href        href
          :header      [:<>
                        [:div {:style {:float "right"}}
                         [OnlineStatusIcon online :corner "top right"]]
                        [ui/IconGroup
                         [ui/Icon {:name "box"}]
                         (when (some #{id} managers)
                           [ui/Icon {:className "fas fa-crown"
                                     :corner    true
                                     :color     "blue"}])]
                        (or name id)]
          :meta        [:<>
                        [:div (str (@tr [:created]) " " (date-string->time-ago created))]
                        (when @creator
                          [:div (str (@tr [:by]) " "
                                     @creator)])
                        (when next-heartbeat-moment
                          [:div (str (@tr [:last-online]) " "
                                     (utils/last-time-online next-heartbeat-moment refresh-interval @locale))])]
          :state       state
          :description (when-not (str/blank? description) description)
          :tags        tags}]))))


(defn orchestrator-icon
  [orchestrator]
  [uix/Icon {:name (get utils/orchestration-icons (keyword orchestrator) "question circle")}])

(defn PreReleaseWarning [{:keys [show? warning-text]}]
  (when show?
    [ui/Popup
     {:trigger        (r/as-element [ui/Icon {:name "exclamation triangle"}])
      :content        warning-text
      :on             "hover"
      :hide-on-scroll true}]))