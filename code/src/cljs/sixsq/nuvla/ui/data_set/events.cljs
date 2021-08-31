(ns sixsq.nuvla.ui.data-set.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.data-set.spec :as spec]
    [sixsq.nuvla.ui.data-set.utils :as utils]))


(reg-event-fx
  ::refresh
  (fn [_ _]
    {:fx [[:dispatch [::get-data-set]]]}))


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::get-data-set nil]}))


(reg-event-db
  ::set-time-period
  (fn [db [_ time-period]]
    (assoc db ::spec/time-period time-period
              ::spec/time-period-filter (utils/create-time-period-filter time-period))))


(reg-event-fx
  ::set-full-text-search
  (fn [{db :db} [_ full-text]]
    {:db       (assoc db ::spec/full-text-search full-text)
     :dispatch [::get-data-set nil]}))


(reg-event-fx
  ::set-data-records
  (fn [{db :db} [_ data-records]]
    {:db (assoc db ::spec/data-records data-records)
     :fx (into [] (for [data-record (:resources data-records)]
                    (when-let [data-object-id (:resource:object data-record)]
                      [:dispatch [::get-data-object data-object-id]])))}))


(reg-event-fx
  ::get-data-records
  (fn [{{:keys [::spec/full-text-search
                ::spec/page
                ::spec/elements-per-page
                ::spec/time-period-filter] :as _db} :db} [_ data-record-filter]]
    {::cimi-api-fx/search [:data-record
                           (utils/get-query-params data-record-filter
                                                   full-text-search
                                                   time-period-filter
                                                   page
                                                   elements-per-page)
                           #(dispatch [::set-data-records %])]}))


(reg-event-db
  ::set-data-set-id
  (fn [db [_ id]]
    (assoc db ::spec/data-set-id id)))


(reg-event-db
  ::set-data-set
  (fn [db [_ data-set]]
    (assoc db ::spec/data-set data-set)))


(reg-event-fx
  ::get-data-set
  (fn [{{:keys [::spec/data-set-id]} :db} _]
    {::cimi-api-fx/get [(str "data-set/" data-set-id)
                        #(do (dispatch [::set-data-set %])
                             (dispatch [::get-data-records (:data-record-filter %)]))]}))


(reg-event-db
  ::set-data-object
  (fn [db [_ {:keys [id] :as data-object}]]
    (assoc-in db [::spec/data-objects id] data-object)))


(reg-event-fx
  ::get-data-object
  (fn [_ [_ data-object-id]]
    {::cimi-api-fx/get [data-object-id
                        #(dispatch [::set-data-object %])]}))
