(ns sixsq.nuvla.ui.data.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.data.spec :as spec]
    [sixsq.nuvla.ui.data.utils :as utils]
    [sixsq.nuvla.ui.deployment-dialog.events :as dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.spec :as dialog-spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-event-db
  ::set-dataset-stats
  (fn [db [_ data-set-id response]]
    (let [doc-count   (get-in response [:aggregations :value_count:id :value])
          total-bytes (get-in response [:aggregations :sum:bytes :value])]
      (-> db
          (assoc-in [::spec/counts data-set-id] doc-count)
          (assoc-in [::spec/sizes data-set-id] total-bytes)))))


(reg-event-fx
  ::fetch-dataset-stats
  (fn [_ [_ data-set-id filter]]
    {::cimi-api-fx/search [:data-record
                           {:filter      filter
                            :select      "id"
                            :aggregation "value_count:id, sum:bytes"
                            :last        0}
                           #(dispatch [::set-dataset-stats data-set-id %])]}))


(reg-event-fx
  ::fetch-all-datasets-stats
  (fn [{{:keys [::spec/credentials
                ::spec/data-sets
                ::spec/full-text-search
                ::spec/time-period-filter]} :db} _]
    (when (seq credentials)
      (let [data-sets-vals (vals data-sets)]
        {:fx (map (fn [data-set]
                    [:dispatch [::fetch-dataset-stats
                                (:id data-set)
                                (general-utils/join-and
                                  time-period-filter full-text-search (:data-record-filter data-set))]])
                  data-sets-vals)}))))


(reg-event-fx
  ::set-time-period
  (fn [{db :db} [_ time-period]]
    {:db       (assoc db ::spec/time-period time-period
                         ::spec/time-period-filter (utils/create-time-period-filter time-period))
     :dispatch [::fetch-all-datasets-stats]}))


(reg-event-fx
  ::set-full-text-search
  (fn [{db :db} [_ full-text]]
    {:db       (assoc db ::spec/full-text-search (general-utils/fulltext-query-string full-text))
     :dispatch [::fetch-all-datasets-stats]}))


(reg-event-db
  ::set-data-records
  (fn [db [_ data-records]]
    (assoc db ::spec/data-records data-records)))


(reg-event-fx
  ::set-credentials
  (fn [{db :db} [_ {credentials :resources}]]
    {:db       (assoc db ::spec/credentials credentials
                         ::spec/counts nil
                         ::spec/sizes nil)
     :dispatch [::fetch-all-datasets-stats]}))


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


(reg-event-fx
  ::set-selected-application-id
  (fn [{db :db} [_ application-id]]
    (dispatch [::dialog-events/create-deployment application-id :data true])
    {:db (assoc db ::spec/selected-application-id application-id)}))


(reg-event-fx
  ::open-application-select-modal
  (fn [{{:keys [::spec/data-sets
                ::spec/selected-data-set-ids] :as db} :db} _]
    (let [selected-data-sets (vals (filter (fn [[k _]]
                                             (boolean (selected-data-set-ids k))) data-sets))
          query-application  (apply general-utils/join-and
                                    (conj (map :module-filter selected-data-sets)
                                          "subtype!='project'"))
          query-objects      (apply general-utils/join-or
                                    (map :data-record-filter selected-data-sets))]
      {:db                  (assoc db ::spec/application-select-visible? true
                                      ::spec/loading-applications? true
                                      ::spec/selected-application-id nil
                                      ::spec/content-type-filter query-objects)
       ::cimi-api-fx/search [:module {:filter query-application}
                             #(dispatch [::set-applications %])]})))


(reg-event-fx
  ::close-application-select-modal
  (fn [{db :db} _]
    {:db (assoc db ::spec/applications nil
                   ::spec/application-select-visible? false)}))


(reg-event-fx
  ::delete-deployment
  (fn [{{:keys [::dialog-spec/deployment]} :db} _]
    {::cimi-api-fx/delete [(:id deployment) #(dispatch [::dialog-events/set-deployment nil])]}))


(reg-event-fx
  ::set-data-sets
  (fn [{db :db} [_ {:keys [resources]}]]
    (let [data-sets (into {} (map (juxt :id identity) resources))]
      {:db       (assoc db ::spec/counts nil
                           ::spec/sizes nil
                           ::spec/data-sets data-sets)
       :dispatch [::fetch-all-datasets-stats]})))


(reg-event-fx
  ::get-data-sets
  (fn [{:keys [db]} _]
    {:db                  (merge db spec/defaults)
     ::cimi-api-fx/search [:data-set {} #(dispatch [::set-data-sets %])]}))


(reg-event-db
  ::toggle-data-set-id
  (fn [{:keys [::spec/selected-data-set-ids] :as db} [_ id]]
    (let [f (if (get selected-data-set-ids id) disj conj)]
      (assoc db ::spec/selected-data-set-ids (f selected-data-set-ids id)))))
