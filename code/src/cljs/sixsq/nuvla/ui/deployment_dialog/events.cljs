(ns sixsq.nuvla.ui.deployment-dialog.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.data.spec :as data-spec]
    [sixsq.nuvla.ui.data.utils :as data-utils]
    [sixsq.nuvla.ui.deployment-dialog.spec :as spec]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-evts]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]))


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
                ::spec/cloud-filter
                ::data-spec/content-type-filter] :as db} :db} [_ {:keys [id] :as credential}]]
    (let [updated-deployment (utils/update-parameter-in-deployment "credential.id" id deployment)
          filter (data-utils/join-and time-period-filter cloud-filter content-type-filter)
          selected-keys (map keyword (::data-spec/selected-data-set-ids db))
          datasets-map (select-keys (::data-spec/data-records-by-data-set db) selected-keys)

          callback-data #(dispatch [::set-deployment
                                    (-> updated-deployment
                                        (assoc :serviceOffers (utils/invert-dataset-map datasets-map))
                                        (assoc-in [:module :content :mounts] (utils/service-offers->mounts %)))])]
      (cond-> {:db (assoc db ::spec/selected-credential credential
                             ::spec/deployment updated-deployment)}
              cloud-filter (assoc ::cimi-api-fx/search
                                  [client :service-offer {:filter filter,
                                                          :select "id, data:bucket, data:nfsIP, data:nfsDevice"}
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
  (fn [{{:keys [::client-spec/client] :as db} :db :as cofx} [_ id first-step do-not-open-modal?]]
    (when client
      (when (= :data first-step)
        (dispatch [::get-service-offers-by-cred]))
      (let [data (if (str/starts-with? id "module/")
                   {:template {:module {:href id}}}
                   {:name               (str "Deployment from " id)
                    :description        (str "A deployment for the deployment template " id)
                    :template {:href id}})
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
        {:db               (assoc db ::spec/loading-deployment? true
                                     ::spec/selected-credential nil
                                     ::spec/deploy-modal-visible? (not (boolean do-not-open-modal?))
                                     ::spec/active-step (or first-step :data)
                                     ::spec/data-step-active? (= first-step :data)
                                     ::spec/cloud-filter nil
                                     ::spec/selected-cloud nil
                                     ::spec/connectors nil
                                     ::spec/data-clouds nil)
         ::cimi-api-fx/add [client "deployment" data add-depl-callback]}))))


(reg-event-fx
  ::get-credentials
  (fn [{{:keys [::client-spec/client
                ::spec/cloud-filter] :as db} :db :as cofx} _]
    (when client
      (let [search-creds-callback #(dispatch [::set-credentials (get % :resources [])])]
        {:db                  (assoc db ::spec/loading-credentials? true
                                        ::spec/credentials nil
                                        ::spec/selected-credential nil)
         ::cimi-api-fx/search [client :credential
                               {:select "id, name, description, created, type"
                                :filter (data-utils/join-and
                                          cloud-filter
                                          (str "type^='cloud-cred'"))} search-creds-callback]}))))


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
      (let [resource-id (:id deployment)
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
  ::set-connectors
  (fn [db [_ {:keys [connectors]}]]
    (assoc db ::spec/connectors (into {} (map (juxt :id identity) connectors)))))


(defn set-cloud-and-filter
  [db cloud]
  (dispatch [::get-credentials])
  (assoc db ::spec/selected-cloud cloud
            ::spec/cloud-filter (str "(connector/href='" cloud "' or connector/href='connector/" cloud "')")))


(reg-event-fx
  ::set-data-clouds
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ data-clouds-response]]
    (let [buckets (get-in data-clouds-response [:aggregations (keyword "terms:connector/href") :buckets])
          clouds (map :key buckets)
          filter (apply data-utils/join-or (map #(str "id='" % "'") clouds))]

      {:db                  (cond-> (assoc db ::spec/data-clouds buckets)
                                    (= 1 (count clouds)) (set-cloud-and-filter (first clouds)))
       ::cimi-api-fx/search [client :connector
                             {:filter filter
                              :select "id, name, description, cloudServiceType"} #(dispatch [::set-connectors %])]})))


(reg-event-fx
  ::get-service-offers-by-cred
  (fn [{{:keys [::client-spec/client
                ::data-spec/time-period-filter
                ::data-spec/cloud-filter
                ::data-spec/content-type-filter
                ::data-spec/credentials] :as db} :db} _]
    (when client

      (let [filter (data-utils/join-and time-period-filter cloud-filter content-type-filter)]
        (-> {:db db}
            (assoc ::cimi-api-fx/search
                   [client :service-offer {:filter      filter
                                           :last        0
                                           :aggregation "terms:connector/href"}
                    #(dispatch [::set-data-clouds %])]))))))


(reg-event-db
  ::set-cloud-filter
  (fn [db [_ cloud]]
    (set-cloud-and-filter db cloud)))
