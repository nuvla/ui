(ns sixsq.nuvla.ui.data-set.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.data-set.spec :as spec]
    [sixsq.nuvla.ui.data-set.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-db
  ::set-page
  (fn [db [_ page]]
    (assoc db ::spec/page page)))


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
    {:db (assoc db ::spec/data-records data-records
                   ::main-spec/loading? false)
     :fx (into [] (for [data-record (:resources data-records)]
                    (when-let [data-object-id (:resource:object data-record)]
                      [:dispatch [::get-data-object data-object-id]])))}))


(reg-event-fx
  ::get-data-records
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page
                ::spec/time-period-filter
                ::spec/data-set
                ::spec/data-record-filter
                ::spec/map-selection
                ::spec/geo-operation]} :db}]
    {::cimi-api-fx/search [:data-record
                           (utils/get-query-params (or
                                                     data-record-filter
                                                     (:data-record-filter data-set))
                                                   (:geojson map-selection)
                                                   geo-operation
                                                   nil
                                                   time-period-filter
                                                   page
                                                   elements-per-page)
                           #(dispatch [::set-data-records %])]}))


(reg-event-db
  ::set-data-set-id
  (fn [db [_ id]]
    (assoc db ::spec/data-set-id id)))


(reg-event-fx
  ::set-data-set
  (fn [{{:keys [::spec/data-set] :as db} :db} [_ new-data-set]]
    (let [data-set-changed? (or (not= (:id new-data-set) (:id data-set))
                                (not= (:updated new-data-set) (:updated data-set)))]
      {:db (cond-> (assoc db ::spec/not-found? (nil? new-data-set)
                             ::spec/data-set new-data-set)
                   data-set-changed? (assoc ::spec/data-record-filter (:data-record-filter new-data-set)))
       :fx [[:dispatch [::get-data-records]]]})))


(reg-event-fx
  ::get-data-set
  (fn [{{:keys [::spec/data-set-id]} :db} _]
    {::cimi-api-fx/get [(str "data-set/" data-set-id)
                        #(dispatch [::set-data-set %])
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


(reg-event-db
  ::set-elements-per-page
  (fn [db [_ elements-per-page]]
    (assoc db ::spec/elements-per-page elements-per-page)))


(reg-event-db
  ::set-data-record-filter
  (fn [db [_ data-record-filter]]
    (assoc db ::spec/data-record-filter data-record-filter)))


(reg-event-fx
  ::set-map-selection
  (fn [{{:keys [::spec/geo-operation] :as db} :db} [_ map-selection]]
    {:db (assoc db ::spec/map-selection map-selection)
     :fx [[:dispatch [::get-data-records]]]}))


(reg-event-fx
  ::set-geo-operation
  (fn [{db :db} [_ op]]
    {:db (assoc db ::spec/geo-operation op)
     :fx [[:dispatch [::get-data-records]]]}))


(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/data-set]} :db}]
    {::cimi-api-fx/delete [(:id data-set) #(dispatch [::history-events/navigate "data"])]}))


(reg-event-db
  ::toggle-data-record-id
  (fn [{:keys [::spec/selected-data-record-ids] :as db} [_ id]]
    (let [f (if (get selected-data-record-ids id) disj conj)]
      (assoc db ::spec/selected-data-record-ids (f selected-data-record-ids id)))))
