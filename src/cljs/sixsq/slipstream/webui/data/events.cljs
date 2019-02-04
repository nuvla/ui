(ns sixsq.slipstream.webui.data.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.data.effects :as fx]
    [sixsq.slipstream.webui.data.spec :as spec]
    [sixsq.slipstream.webui.data.utils :as utils]
    [sixsq.slipstream.webui.deployment-dialog.events :as dialog-events]
    [sixsq.slipstream.webui.deployment-dialog.spec :as dialog-spec]))


(defn fetch-data-cofx
  [credentials client time-period-filter cloud-filter full-text-search datasets]
  (if (empty? credentials)
    {}
    {::fx/fetch-data [client time-period-filter cloud-filter full-text-search (vals datasets)
                      #(dispatch [::set-data %1 %2])]}))


(reg-event-fx
  ::set-time-period
  (fn [{{:keys [::client-spec/client
                ::spec/cloud-filter
                ::spec/credentials
                ::spec/datasets
                ::spec/full-text-search] :as db} :db} [_ time-period]]
    (let [time-period-filter (utils/create-time-period-filter time-period)]
      (merge {:db (assoc db ::spec/time-period time-period
                            ::spec/time-period-filter time-period-filter)}
             (fetch-data-cofx credentials client time-period-filter cloud-filter full-text-search datasets)))))


(reg-event-fx
  ::set-full-text-search
  (fn [{{:keys [::client-spec/client
                ::spec/cloud-filter
                ::spec/credentials
                ::spec/datasets
                ::spec/time-period-filter] :as db} :db} [_ full-text-search]]
    (let [full-text-query (when (and full-text-search (not (str/blank? full-text-search)))
                            (str "fulltext=='" full-text-search "*'"))]
      (merge {:db (assoc db ::spec/full-text-search full-text-query)}
             (fetch-data-cofx credentials client time-period-filter cloud-filter full-text-query datasets)))))


(reg-event-db
  ::set-service-offers
  (fn [db [_ service-offers]]
    (assoc db ::spec/service-offers service-offers)))


(reg-event-db
  ::set-data
  (fn [db [_ dataset-id response]]
    (let [doc-count (get-in response [:aggregations :count:id :value])
          total-bytes (get-in response [:aggregations :sum:data:bytes :value])
          service-offer-ids (mapv :id (:serviceOffers response))]
      (-> db
          (update ::spec/counts assoc dataset-id doc-count)
          (update ::spec/sizes assoc dataset-id total-bytes)
          (update ::spec/service-offers-by-dataset assoc (keyword dataset-id) service-offer-ids)))))


(reg-event-fx
  ::set-credentials
  (fn [{{:keys [::client-spec/client
                ::spec/time-period-filter
                ::spec/datasets
                ::spec/full-text-search] :as db} :db} [_ {:keys [credentials]}]]
    (let [cloud-filter (utils/create-cloud-filter credentials)]
      (when client
        (merge {:db (assoc db ::spec/credentials credentials
                              ::spec/cloud-filter cloud-filter
                              ::spec/counts nil
                              ::spec/sizes nil)}
               (fetch-data-cofx credentials client time-period-filter cloud-filter full-text-search datasets))))))


(reg-event-fx
  ::get-credentials
  (fn [{{:keys [::client-spec/client] :as db} :db} _]
    (when client
      {:db                  (assoc db ::spec/credentials nil)
       ::cimi-api-fx/search [client "credentials" {:$filter "type^='cloud-cred'"
                                                   :$select "id, name, connector"}
                             #(dispatch [::set-credentials %])]})))


(reg-event-db
  ::set-applications
  (fn [db [_ applications]]
    (assoc db ::spec/applications (:modules applications)
              ::spec/loading-applications? false)))


(reg-event-fx
  ::set-selected-application-id
  (fn [{{:keys [::client-spec/client
                ::dialog-spec/deployment] :as db} :db} [_ application-id]]

    (dispatch [::dialog-events/create-deployment application-id :data true])

    (cond-> {:db (assoc db ::spec/selected-application-id application-id)}
            (:id deployment) (assoc ::cimi-api-fx/delete [client (:id deployment)
                                                          #(dispatch [::dialog-events/set-deployment nil])]))))


(reg-event-fx
  ::open-application-select-modal
  (fn [{{:keys [::client-spec/client
                ::spec/datasets
                ::spec/selected-dataset-ids] :as db} :db} _]
    (let [selected-datasets (vals (filter (fn [[k v]] (boolean (selected-dataset-ids k))) datasets))
          query-application (apply utils/join-and (map (keyword "dataset:applicationFilter") selected-datasets))
          query-objects (apply utils/join-or (map (keyword "dataset:objectFilter") selected-datasets))]
      {:db                  (assoc db ::spec/application-select-visible? true
                                      ::spec/loading-applications? true
                                      ::spec/selected-application-id nil
                                      ::spec/content-type-filter query-objects)
       ::cimi-api-fx/search [client "modules" {:$filter query-application}
                             #(dispatch [::set-applications %])]})))


(reg-event-fx
  ::close-application-select-modal
  (fn [{{:keys [::client-spec/client
                ::dialog-spec/deployment] :as db} :db} _]
    {:db (assoc db ::spec/applications nil
                   ::spec/application-select-visible? false)}))

(reg-event-fx
  ::delete-deployment
  (fn [{{:keys [::client-spec/client
                ::dialog-spec/deployment] :as db} :db} _]
    {::cimi-api-fx/delete [client (:id deployment) #(dispatch [::dialog-events/set-deployment nil])]}))


(reg-event-fx
  ::set-datasets
  (fn [{{:keys [::client-spec/client
                ::spec/credentials
                ::spec/cloud-filter
                ::spec/time-period-filter
                ::spec/datasets
                ::spec/full-text-search] :as db} :db} [_ {:keys [serviceOffers]}]]
    (let []
      (assoc db ::spec/datasets datasets))
    (let [datasets (into {} (map (juxt :id identity) serviceOffers))]
      (when client
        (merge {:db (assoc db ::spec/counts nil
                              ::spec/sizes nil
                              ::spec/datasets datasets)}
               (fetch-data-cofx credentials client time-period-filter cloud-filter full-text-search datasets))))))


(reg-event-fx
  ::get-datasets
  (fn [{{:keys [::client-spec/client] :as db} :db} _]
    (when client
      {:db                  (assoc db ::spec/datasets nil)
       ::cimi-api-fx/search [client "serviceOffers" {:$filter "resource:type='DATASET'"}
                             #(dispatch [::set-datasets %])]})))


(reg-event-db
  ::toggle-dataset-id
  (fn [{:keys [::spec/selected-dataset-ids] :as db} [_ id]]
    (let [f (if (get selected-dataset-ids id) disj conj)]
      (assoc db ::spec/selected-dataset-ids (f selected-dataset-ids id)))))
