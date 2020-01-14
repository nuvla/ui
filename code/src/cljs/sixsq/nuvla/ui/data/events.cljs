(ns sixsq.nuvla.ui.data.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.data.effects :as fx]
    [sixsq.nuvla.ui.data.spec :as spec]
    [sixsq.nuvla.ui.data.utils :as utils]
    [sixsq.nuvla.ui.deployment-dialog.events :as dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.spec :as dialog-spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(defn fetch-data-cofx
  [cofx credentials time-period-filter full-text-search data-sets]
  (if (empty? credentials)
    cofx
    (assoc cofx ::fx/fetch-data
                [time-period-filter full-text-search (vals data-sets)
                 #(dispatch [::set-data %1 %2])])))


(reg-event-fx
  ::set-time-period
  (fn [{{:keys [::spec/credentials
                ::spec/data-sets
                ::spec/full-text-search] :as db} :db} [_ time-period]]
    (let [time-period-filter (utils/create-time-period-filter time-period)]
      (-> {:db (assoc db ::spec/time-period time-period
                         ::spec/time-period-filter time-period-filter)}
          (fetch-data-cofx credentials
                           time-period-filter
                           full-text-search
                           data-sets)))))


(reg-event-fx
  ::set-full-text-search
  (fn [{{:keys [::spec/credentials
                ::spec/data-sets
                ::spec/time-period-filter] :as db} :db} [_ full-text]]
    (let [full-text-query (general-utils/fulltext-query-string full-text)]
      (-> {:db (assoc db ::spec/full-text-search full-text-query)}
          (fetch-data-cofx credentials
                           time-period-filter
                           full-text-query
                           data-sets)))))


(reg-event-db
  ::set-data-records
  (fn [db [_ data-records]]
    (assoc db ::spec/data-records data-records)))


(reg-event-db
  ::set-data
  (fn [db [_ data-set-id response]]
    (let [doc-count       (get-in response [:aggregations :value_count:id :value])
          total-bytes     (get-in response [:aggregations :sum:bytes :value])
          data-record-ids (mapv :id (:resources response))]
      (-> db
          (update ::spec/counts assoc data-set-id doc-count)
          (update ::spec/sizes assoc data-set-id total-bytes)
          (update ::spec/data-records-by-data-set assoc (keyword data-set-id) data-record-ids)))))


(reg-event-fx
  ::set-credentials
  (fn [{{:keys [::spec/time-period-filter
                ::spec/data-sets
                ::spec/full-text-search] :as db} :db} [_ {credentials :resources}]]
    (-> {:db (assoc db ::spec/credentials credentials
                       ::spec/counts nil
                       ::spec/sizes nil)}
        (fetch-data-cofx credentials
                         time-period-filter
                         full-text-search
                         data-sets))))


(reg-event-fx
  ::get-credentials
  (fn [{:keys [db]} _]
    {:db                  (assoc db ::spec/credentials nil)
     ::cimi-api-fx/search [:credential {:filter "subtype^='infrastructure-service-swarm'"
                                        :select "id, name, services"}
                           #(dispatch [::set-credentials %])]}))


(reg-event-db
  ::set-applications
  (fn [db [_ applications]]
    (assoc db ::spec/applications (:resources applications)
              ::spec/loading-applications? false)))


(reg-event-db
  ::set-selected-application-id
  (fn [{:keys [::dialog-spec/deployment] :as db} [_ application-id]]
    (dispatch [::dialog-events/create-deployment application-id :data true])
    (assoc db ::spec/selected-application-id application-id)))


(reg-event-fx
  ::open-application-select-modal
  (fn [{{:keys [::spec/data-sets
                ::spec/selected-data-set-ids] :as db} :db} _]
    (let [selected-data-sets (vals (filter (fn [[k v]] (boolean (selected-data-set-ids k))) data-sets))
          query-application  (apply general-utils/join-and (map :module-filter selected-data-sets))
          query-objects      (apply general-utils/join-or (map :data-record-filter selected-data-sets))]
      {:db                  (assoc db ::spec/application-select-visible? true
                                      ::spec/loading-applications? true
                                      ::spec/selected-application-id nil
                                      ::spec/content-type-filter query-objects)
       ::cimi-api-fx/search [:module {:filter query-application}
                             #(dispatch [::set-applications %])]})))


(reg-event-fx
  ::close-application-select-modal
  (fn [{{:keys [::dialog-spec/deployment] :as db} :db} _]
    {:db (assoc db ::spec/applications nil
                   ::spec/application-select-visible? false)}))


(reg-event-fx
  ::delete-deployment
  (fn [{{:keys [::dialog-spec/deployment] :as db} :db} _]
    {::cimi-api-fx/delete [(:id deployment) #(dispatch [::dialog-events/set-deployment nil])]}))


(reg-event-fx
  ::set-data-sets
  (fn [{{:keys [::spec/credentials
                ::spec/time-period-filter
                ::spec/data-sets
                ::spec/full-text-search] :as db} :db} [_ {:keys [resources]}]]
    (let [data-sets (into {} (map (juxt :id identity) resources))]
      (-> {:db (assoc db ::spec/counts nil
                         ::spec/sizes nil
                         ::spec/data-sets data-sets)}
          (fetch-data-cofx credentials
                           time-period-filter
                           full-text-search
                           data-sets)))))


(reg-event-fx
  ::get-data-sets
  (fn [{:keys [db]} _]
    {:db                  (assoc db ::spec/data-sets nil)
     ::cimi-api-fx/search [:data-set {} #(dispatch [::set-data-sets %])]}))


(reg-event-db
  ::toggle-data-set-id
  (fn [{:keys [::spec/selected-data-set-ids] :as db} [_ id]]
    (let [f (if (get selected-data-set-ids id) disj conj)]
      (assoc db ::spec/selected-data-set-ids (f selected-data-set-ids id)))))
