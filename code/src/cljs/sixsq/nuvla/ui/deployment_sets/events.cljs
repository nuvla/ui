(ns sixsq.nuvla.ui.deployment-sets.events
  (:require [re-frame.core :refer [dispatch reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployment-sets.spec :as spec]
            [sixsq.nuvla.ui.deployment-sets.subs :as subs]
            [sixsq.nuvla.ui.deployment-sets-detail.events :as depl-group-events]
            [sixsq.nuvla.ui.deployment-sets-detail.subs :as depl-group-subs]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

(def refresh-id :dep-sets-get-deployment-sets)
(def refresh-summary-id :dep-sets-get-deployment-sets-summary)


(reg-event-fx
  ::refresh
  (fn []
    {:fx [[:dispatch [::main-events/action-interval-start
                      {:id        refresh-id
                       :frequency 10000
                       :event     [::get-deployment-sets]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-summary-id
                       :frequency 10000
                       :event     [::get-deployment-sets-summary]}]]]}))

(defn state-filter
  [state]
  (case state
    "STARTED" "state='STARTED' or state='UPDATED'"
    "PARTIAL" "state^='PARTIALLY'"
    "PENDING" "state='STARTING' or state='UPDATING' or state='STOPPING'"
    (str "state='" state "'")))

(reg-event-fx
  ::get-deployment-sets
  (fn [{{:keys [::spec/state-selector] :as db} :db}]
    (let [params (pagination-plugin/first-last-params
                   db [::spec/pagination]
                   {:orderby "created:desc"
                    :filter  (general-utils/join-and
                               (when state-selector
                                 (state-filter state-selector))
                               (full-text-search-plugin/filter-text
                                 db [::spec/search]))})]
      {::cimi-api-fx/search [:deployment-set params
                             #(dispatch [::set-deployment-sets %])]})))

(reg-event-fx
  ::set-deployment-sets
  (fn [{:keys [db]} [_ deployment-sets]]
    (if (instance? js/Error deployment-sets)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info deployment-sets)]
                   {:header  (cond-> (str "failure getting deployment-sets")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db (assoc db ::spec/deployment-sets deployment-sets
                     ::main-spec/loading? false)})))

(reg-event-fx
  ::set-deployment-sets-summary
  (fn [{db :db} [_ deployment-sets-summary]]
    {:db (assoc db ::spec/deployment-sets-summary deployment-sets-summary)}))

(defn get-query-aggregation-params
  [full-text-search aggregation extra]
  {:first       0
   :last        0
   :aggregation aggregation
   :filter      (general-utils/join-and
                  full-text-search
                  (when extra extra))})

(reg-event-fx
  ::get-deployment-sets-summary
  (fn [{db :db} _]
    {::cimi-api-fx/search [:deployment-set
                           (get-query-aggregation-params
                             (full-text-search-plugin/filter-text
                               db [::spec/search])
                             "terms:state"
                             nil)
                           #(dispatch [::set-deployment-sets-summary %])]}))

(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    {:db (assoc db ::spec/state-selector state-selector)
     :fx [[:dispatch [::pagination-plugin/change-page [::spec/pagination] 1]]]}))

(reg-event-fx
  ::new-deployment-set
  (fn [_ _]
    (let [id (random-uuid)]
      {:fx [[:dispatch [::routing-events/navigate
                        routes/deployment-groups-details
                        {:uuid :create}
                        {::subs/creation-temp-id-key id}]]]})))

(reg-event-fx
  ::create-deployment-set-from-apps-set
  (fn [_ [_ module-id]]
    (let [id (random-uuid)]
      {:fx [[:dispatch [::depl-group-events/fetch-apps-set-add-apps module-id]]
            [:dispatch [::routing-events/navigate
                        routes/deployment-groups-details
                        {:uuid :create}
                        {depl-group-subs/creation-temp-id-key id}]]]})))

