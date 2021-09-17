(ns sixsq.nuvla.ui.data-set.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.data-set.spec :as spec]
    [sixsq.nuvla.ui.data-set.utils :as utils]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-fx
  ::refresh
  (fn [_ _]
    {:fx [[:dispatch [::get-data-set]]]}))


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::get-data-set]}))


(reg-event-db
  ::set-time-period
  (fn [db [_ time-period]]
    (assoc db ::spec/time-period time-period
              ::spec/time-period-filter (utils/create-time-period-filter time-period))))


(reg-event-fx
  ::set-full-text-search
  (fn [{db :db} [_ full-text]]
    {:db       (assoc db ::spec/full-text-search full-text)
     :dispatch [::get-data-set]}))


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
    (assoc db ::spec/not-found? (nil? data-set)
              ::spec/data-set data-set
              ::main-spec/loading? false)))


(reg-event-fx
  ::get-data-set
  (fn [{{:keys [::spec/data-set-id]} :db} _]
    {::cimi-api-fx/get [(str "data-set/" data-set-id)
                        #(do (dispatch [::set-data-set %])
                             (dispatch [::get-data-records (:data-record-filter %)]))
                        :on-error #(dispatch [::set-data-set nil])]}))


(reg-event-db
  ::set-data-object
  (fn [db [_ {:keys [id] :as data-object}]]
    (assoc-in db [::spec/data-objects id] data-object)))


(reg-event-fx
  ::get-data-object
  (fn [_ [_ data-object-id]]
    {::cimi-api-fx/get [data-object-id
                        #(dispatch [::set-data-object %])]}))


(reg-event-fx
  ::edit
  (fn [_ [_ resource-id data success-msg]]
    {::cimi-api-fx/edit [resource-id data
                         #(if (instance? js/Error %)
                            (let [{:keys [status message]} (response/parse-ex-info %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "error editing " resource-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :error}]))
                            (do
                              (when success-msg
                                (dispatch [::messages-events/add
                                           {:header  success-msg
                                            :content success-msg
                                            :type    :success}]))
                              (dispatch [::set-data-set %])))]}))
