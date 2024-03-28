(ns sixsq.nuvla.ui.pages.edges.views-utils
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.pages.edges.events :as events]
            [sixsq.nuvla.ui.pages.edges.spec :as spec]
            [sixsq.nuvla.ui.pages.edges.subs :as subs]
            [sixsq.nuvla.ui.pages.edges.utils :as utils]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.view-components :refer [OnlineStatusIcon]]))


(defn NuvlaboxRow
  [{:keys [id name description created state tags online] :as _nuvlabox} managers]
  (let [locale (subscribe [::i18n-subs/locale])
        uuid   (general-utils/id->uuid id)]
    [ui/TableRow {:on-click #(dispatch [::routing-events/navigate (utils/edges-details-url uuid)])
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
        [icons/CheckIconFull])]]))


(defn AddButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/MenuItem
     {:name     (@tr [:add])
      :icon     icons/i-plus-large
      :on-click #(dispatch
                   [::main-events/subscription-required-dispatch
                    [::events/open-modal spec/modal-add-id]])}]))

(defn- date-string->time-ago [created]
  (-> created time/parse-iso8601 time/ago))


(defn NuvlaboxCard
  [_nuvlabox _managers]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [id name description created state tags online created-by] :as nuvlabox} managers]
      (let [href                  (name->href routes/edges-details {:uuid (general-utils/id->uuid id)})
            last-heartbeat-moment @(subscribe [::subs/last-online nuvlabox])
            creator               (subscribe [::session-subs/resolve-user created-by])]
        ^{:key id}
        [uix/Card
         {:href        href
          :header      [:<>
                        [:div {:style {:float "right"}}
                         [OnlineStatusIcon online :corner "top right"]]
                        [ui/IconGroup
                         [icons/BoxIcon]
                         (when (some #{id} managers)
                           [icons/CrownIcon {:corner    true
                                             :color     "blue"}])]
                        (or name id)]
          :meta        [:<>
                        [:div (str (@tr [:created]) " " (date-string->time-ago created))]
                        (when @creator
                          [:div (str (@tr [:by]) " "
                                     @creator)])
                        (when last-heartbeat-moment
                          [:div (str (@tr [:last-online]) " ")
                                [uix/TimeAgo last-heartbeat-moment]])]
          :state       state
          :description (when-not (str/blank? description) description)
          :tags        tags}]))))


(defn orchestrator-icon
  [orchestrator]
  [icons/Icon {:name (get utils/orchestration-icons (keyword orchestrator) "question circle")}])
