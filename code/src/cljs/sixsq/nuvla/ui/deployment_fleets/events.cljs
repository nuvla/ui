(ns sixsq.nuvla.ui.deployment-fleets.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.deployment-fleets.spec :as spec]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]))

(def refresh-id :dep-fleets-get-deployment-fleets)
(def refresh-summary-id :dep-fleets-get-deployment-fleets-summary)


(reg-event-fx
  ::refresh
  (fn [_ _]
    {:fx [[:dispatch [::main-events/action-interval-start
                      {:id        refresh-id
                       :frequency 10000
                       :event     [::get-deployment-fleets]}]]
          [:dispatch [::main-events/action-interval-start
                      {:id        refresh-summary-id
                       :frequency 10000
                       :event     [::get-deployment-fleets-summary]}]]]}))


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::refresh]}))


(reg-event-fx
  ::set-full-text-search
  (fn [{db :db} [_ full-text-search]]
    {:db       (assoc db ::spec/full-text-search full-text-search
                         ::spec/page 1)
     :dispatch [::refresh]}))

(defn state-filter
  [state]
  (case state
    "PENDING" "state='CREATING' or state='STARTING' or state='STOPPING'"
    (str "state='" state "'")))

(defn get-query-params
  [full-text-search page elements-per-page state-selector]
  {:first   (inc (* (dec page) elements-per-page))
   :last    (* page elements-per-page)
   :orderby "created:desc"
   :filter  (general-utils/join-and
              (when state-selector (state-filter state-selector))
              (general-utils/fulltext-query-string full-text-search))})

(reg-event-fx
  ::get-deployment-fleets
  (fn [{{:keys [::spec/state-selector
                ::spec/page
                ::spec/elements-per-page
                ::spec/full-text-search] :as _db} :db} _]
    {::cimi-api-fx/search [:deployment-fleet
                           (get-query-params
                             full-text-search
                             page
                             elements-per-page
                             state-selector)
                           #(dispatch [::set-deployment-fleets %])]}))

(reg-event-fx
  ::set-deployment-fleets
  (fn [{:keys [db]} [_ deployment-fleets]]
    (if (instance? js/Error deployment-fleets)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info deployment-fleets)]
                   {:header  (cond-> (str "failure getting deployment-fleets")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db (assoc db ::spec/deployment-fleets deployment-fleets
                     ::main-spec/loading? false)})))

(reg-event-fx
  ::set-deployment-fleets-summary
  (fn [{db :db} [_ deployment-fleets-summary]]
    {:db (assoc db ::spec/deployment-fleets-summary deployment-fleets-summary)}))

(defn get-query-aggregation-params
  [full-text-search aggregation extra]
  {:first       0
   :last        0
   :aggregation aggregation
   :filter      (general-utils/join-and
                  (general-utils/fulltext-query-string full-text-search)
                  (when extra extra))})

(reg-event-fx
  ::get-deployment-fleets-summary
  (fn [{{:keys [::spec/full-text-search] :as _db} :db} _]
    {::cimi-api-fx/search [:deployment-fleet
                           (get-query-aggregation-params
                             full-text-search
                             "terms:state"
                             nil)
                           #(dispatch [::set-deployment-fleets-summary %])]}))

(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    {:db (assoc db ::spec/state-selector state-selector
                   ::spec/page 1)
     :fx [[:dispatch [::get-deployment-fleets]]]}))
