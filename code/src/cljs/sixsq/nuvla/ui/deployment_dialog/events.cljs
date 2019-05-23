(ns sixsq.nuvla.ui.deployment-dialog.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.data.spec :as data-spec]
    [sixsq.nuvla.ui.data.utils :as data-utils]
    [sixsq.nuvla.ui.deployment-dialog.spec :as spec]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-evts]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


(reg-event-fx
  ::close-deploy-modal
  (fn [{{:keys [::client-spec/client
                ::spec/deployment] :as db} :db :as cofx} _]
    (cond-> {:db                  (assoc db ::spec/deploy-modal-visible? false
                                            ::spec/deployment nil)
             ::cimi-api-fx/delete [client (:id deployment) #()]})))


(reg-event-db
  ::set-credentials
  (fn [db [_ credentials]]
    (when (= 1 (count credentials))
      (dispatch [::set-selected-credential (first credentials)]))

    (assoc db ::spec/credentials credentials
              ::spec/loading-credentials? false)))


(reg-event-fx
  ::set-selected-credential
  (fn [{{:keys [::client-spec/client
                ::spec/deployment
                ::data-spec/time-period-filter
                ::spec/infra-service-filter
                ::data-spec/content-type-filter] :as db} :db} [_ {credential-id :id :as credential}]]
    (let [updated-deployment (assoc deployment :credential-id credential-id)
          filter             (data-utils/join-and time-period-filter infra-service-filter content-type-filter)
          selected-keys      (map keyword (::data-spec/selected-data-set-ids db))
          datasets-map       (select-keys (::data-spec/data-records-by-data-set db) selected-keys)

          callback-data      (fn [{:keys [resources] :as data-records}]
                               (let [distinct-mounts (->> resources
                                                          (map :mount)
                                                          (distinct))]
                                 (dispatch [::set-deployment
                                            (-> updated-deployment
                                                (assoc :data-records (utils/invert-dataset-map datasets-map))
                                                (assoc-in [:module :content :mounts] distinct-mounts))])))]
      (cond-> {:db (assoc db ::spec/selected-credential credential
                             ::spec/deployment updated-deployment)}
              infra-service-filter (assoc ::cimi-api-fx/search
                                          [client :data-record {:filter filter,
                                                                :select "id, mount"}
                                           callback-data])))))


(reg-event-db
  ::set-active-step
  (fn [db [_ active-step]]
    (assoc db ::spec/active-step active-step)))


(reg-event-db
  ::set-deployment
  (fn [db [_ deployment]]
    (assoc db ::spec/deployment deployment
              ::spec/loading-deployment? false)))


(reg-event-fx
  ::get-deployment
  (fn [{{:keys [::client-spec/client] :as db} :db :as cofx} [_ id]]
    (when client
      {:db               (assoc db ::spec/deployment {:id id})
       ::cimi-api-fx/get [client id #(dispatch [::set-deployment %])]})))


(reg-event-fx
  ::create-deployment
  (fn [{{:keys [::client-spec/client
                ::spec/deployment] :as db} :db :as cofx} [_ id first-step do-not-open-modal?]]
    (when client
      (when (= :data first-step)
        (dispatch [::get-data-records-by-cred]))
      (let [data              {:module {:href id}}
            old-deployment-id (:id deployment)
            add-depl-callback (fn [response]
                                (if (instance? js/Error response)
                                  (let [{:keys [status message]} (response/parse-ex-info response)]
                                    (dispatch [::messages-events/add
                                               {:header  (cond-> (str "error create deployment")
                                                                 status (str " (" status ")"))
                                                :content message
                                                :type    :error}])
                                    (dispatch [::close-deploy-modal]))
                                  (do
                                    (dispatch [::get-credentials])
                                    (dispatch [::get-deployment (:resource-id response)]))))]
        (cond-> {:db               (assoc db ::spec/loading-deployment? true
                                             ::spec/deployment nil
                                             ::spec/selected-credential nil
                                             ::spec/deploy-modal-visible? (not (boolean do-not-open-modal?))
                                             ::spec/active-step (or first-step :data)
                                             ::spec/data-step-active? (= first-step :data)
                                             ::spec/infra-service-filter nil
                                             ::spec/selected-infra-service nil
                                             ::spec/infra-services nil
                                             ::spec/data-infra-services nil)
                 ::cimi-api-fx/add [client :deployment data add-depl-callback]}
                old-deployment-id (assoc ::cimi-api-fx/delete [client old-deployment-id #()]))))))


(reg-event-fx
  ::get-credentials
  (fn [{{:keys [::client-spec/client
                ::spec/selected-infra-service] :as db} :db :as cofx} _]
    (when client
      (let [search-creds-callback #(dispatch [::set-credentials (get % :resources [])])]
        {:db                  (assoc db ::spec/loading-credentials? true
                                        ::spec/credentials nil
                                        ::spec/selected-credential nil)
         ::cimi-api-fx/search [client :credential
                               {:select "id, name, description, created, subtype"
                                :filter (data-utils/join-and
                                          (when selected-infra-service
                                            (str "parent='" selected-infra-service "'"))
                                          (str "subtype='infrastructure-service-swarm'"))} search-creds-callback]}))))


(reg-event-fx
  ::start-deployment
  (fn [{{:keys [::client-spec/client] :as db} :db :as cofx} [_ id]]
    (when client
      (let [start-callback (fn [response]
                             (if (instance? js/Error response)
                               (let [{:keys [status message]} (response/parse-ex-info response)]
                                 (dispatch [::messages-events/add
                                            {:header  (cond-> (str "error start " id)
                                                              status (str " (" status ")"))
                                             :content message
                                             :type    :error}]))
                               (let [{:keys [status message resource-id]} (response/parse response)
                                     success-msg {:header  (cond-> (str "started " resource-id)
                                                                   status (str " (" status ")"))
                                                  :content message
                                                  :type    :success}]
                                 (dispatch [::messages-events/add success-msg])
                                 (dispatch [::history-evts/navigate "deployment"]))))]
        {:db                     (assoc db ::spec/deploy-modal-visible? false)
         ::cimi-api-fx/operation [client id "start" start-callback]}))))


(reg-event-fx
  ::edit-deployment
  (fn [{{:keys [::client-spec/client
                ::spec/deployment] :as db} :db :as cofx} _]
    (when client
      (let [resource-id   (:id deployment)
            edit-callback (fn [response]
                            (if (instance? js/Error response)
                              (let [{:keys [status message]} (response/parse-ex-info response)]
                                (dispatch [::messages-events/add
                                           {:header  (cond-> (str "error editing " resource-id)
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :error}]))
                              (dispatch [::start-deployment resource-id])))]
        {::cimi-api-fx/edit [client resource-id deployment edit-callback]}))))


(reg-event-db
  ::set-infra-services
  (fn [db [_ {infra-services :resources}]]
    (assoc db ::spec/infra-services (into {} (map (juxt :id identity) infra-services)))))


(defn set-infra-service-and-filter
  [db infra-service]
  (dispatch [::get-credentials])
  (assoc db ::spec/selected-infra-service infra-service
            ::spec/infra-services-filter (str "infrastructure-services='" infra-service "'")
            ::spec/infra-service-filter (str "infrastructure-service='" infra-service "'")))


(reg-event-fx
  ::set-data-infra-services
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ data-clouds-response]]
    (let [buckets        (get-in data-clouds-response [:aggregations (keyword "terms:infrastructure-service") :buckets])
          infra-services (map :key buckets)
          filter         (apply data-utils/join-or (map #(str "id='" % "'") infra-services))]

      {:db                  (cond-> (assoc db ::spec/data-infra-services buckets)
                                    (= 1 (count infra-services)) (set-infra-service-and-filter (first infra-services)))
       ::cimi-api-fx/search [client :infrastructure-service
                             {:filter filter
                              :select "id, name, description, subtype"} #(dispatch [::set-infra-services %])]})))


(reg-event-fx
  ::get-data-records-by-cred
  (fn [{{:keys [::client-spec/client
                ::data-spec/time-period-filter
                ::data-spec/infra-services-filter
                ::data-spec/content-type-filter
                ::data-spec/credentials] :as db} :db} _]
    (when client
      (let [filter (data-utils/join-and time-period-filter infra-services-filter content-type-filter)]
        {:db db
         ::cimi-api-fx/search
             [client :data-record {:filter      filter
                                   :last        0
                                   :aggregation "terms:infrastructure-service"}
              #(dispatch [::set-data-infra-services %])]}))))


(reg-event-db
  ::set-infra-service-filter
  (fn [db [_ cloud]]
    (set-infra-service-and-filter db cloud)))
