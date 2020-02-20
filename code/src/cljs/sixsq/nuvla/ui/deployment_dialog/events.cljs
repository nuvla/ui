(ns sixsq.nuvla.ui.deployment-dialog.events
  (:require
    [re-frame.core :refer [dispatch inject-cofx reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.data.spec :as data-spec]
    [sixsq.nuvla.ui.deployment-dialog.spec :as spec]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-fx
  ::close-deploy-modal
  (fn [{{:keys [::spec/deployment] :as db} :db} _]
    {:db                  (merge db spec/defaults)
     ::cimi-api-fx/delete [(:id deployment) #()]}))


(reg-event-fx
  ::set-credentials
  (fn [{{:keys [::spec/selected-credential-id] :as db} :db} [_ {credentials :resources}]]
    (let [only-one-cred (= (count credentials) 1)
          first-cred    (when only-one-cred (first credentials))
          select-cred?  (and only-one-cred
                             (not= selected-credential-id (:id first-cred)))]
      (cond-> {:db (assoc db ::spec/credentials credentials
                             ::spec/credentials-loading? false)}
              select-cred? (assoc :dispatch [::set-selected-credential (first credentials)])))))


(reg-event-fx
  ::get-data-records-for-cred
  (fn [{{:keys [::spec/deployment
                ::data-spec/time-period-filter
                ::spec/cloud-filter
                ::data-spec/content-type-filter] :as db} :db} _]
    (let [filter        (general-utils/join-and time-period-filter
                                                cloud-filter
                                                content-type-filter)
          selected-keys (map keyword (::data-spec/selected-data-set-ids db))
          datasets-map  (select-keys (::data-spec/data-records-by-data-set db) selected-keys)

          callback-data (fn [{:keys [resources] :as data-records}]
                          (let [distinct-mounts (->> resources
                                                     (map :mount)
                                                     (remove nil?)
                                                     (distinct))]
                            (dispatch [::set-deployment
                                       (cond->
                                         (assoc deployment
                                           :data-records (utils/invert-dataset-map datasets-map))

                                         (seq distinct-mounts) (assoc-in
                                                                 [:module :content :mounts]
                                                                 distinct-mounts))])))]
      {::cimi-api-fx/search [:data-record {:filter filter, :select "id, mount"} callback-data]})))


(reg-event-fx
  ::set-job-check-cred
  (fn [{{:keys [::spec/job-check-cred-id
                ::spec/selected-infra-service]
         :as   db} :db} [_ {:keys [id return-code] :as resource}]]
    (when (= job-check-cred-id id)
      (cond-> {:db (cond-> (assoc db ::spec/check-cred resource)
                           return-code (assoc ::spec/job-check-cred-id nil
                                              ::spec/check-cred-loading? false))}
              return-code (assoc :dispatch [::refresh-credentials])
              (nil? return-code) (assoc :dispatch-later
                                        [{:ms 3000 :dispatch [::get-job-check-cred]}])))))


(reg-event-fx
  ::get-job-check-cred
  (fn [{{:keys [::spec/job-check-cred-id]} :db} _]
    {::cimi-api-fx/get [job-check-cred-id #(dispatch [::set-job-check-cred %])]}))


(reg-event-fx
  ::set-check-cred-job-id
  (fn [{:keys [db]} [_ job-id]]
    {:db             (assoc db ::spec/job-check-cred-id job-id)
     :dispatch-later [{:ms 3000 :dispatch [::get-job-check-cred]}]}))


(reg-event-fx
  ::set-selected-credential
  (fn [{{:keys [::spec/deployment
                ::spec/data-step-active?] :as db} :db} [_ {:keys [id] :as credential}]]
    (let [check-cred? (utils/credential-need-check? credential)]
      (cond-> {:db (assoc db ::spec/selected-credential-id id
                             ::spec/deployment (assoc deployment :parent id)
                             ::spec/check-cred-loading? check-cred?
                             ::spec/check-cred nil)}

              data-step-active? (assoc :dispatch [::get-data-records-for-cred])
              check-cred? (assoc ::cimi-api-fx/operation
                                 [id
                                  "check"
                                  #(dispatch [::set-check-cred-job-id (:location %)])])))))


(reg-event-db
  ::set-active-step
  (fn [db [_ active-step]]
    (assoc db ::spec/active-step active-step)))


(reg-event-fx
  ::set-selected-infra-service
  (fn [{:keys [db]} [_ infra-service]]
    {:db       (assoc db ::spec/selected-infra-service infra-service
                         ::spec/check-cred nil
                         ::spec/check-cred-loading? false)
     :dispatch [::get-credentials]}))


(reg-event-fx
  ::set-infra-services
  (fn [{:keys [db]} [_ {infra-services :resources}]]
    (cond-> {:db (assoc db ::spec/infra-services infra-services
                           ::spec/infra-services-loading? false)}
            (= (count infra-services) 1) (assoc :dispatch [::set-selected-infra-service
                                                           (first infra-services)]))))


(reg-event-fx
  ::get-infra-services
  (fn [{:keys [db]} [_ filter]]
    {:db                  (assoc db ::spec/infra-services-loading? true
                                    ::spec/infra-services nil
                                    ::spec/selected-infra-service nil)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter filter,
                            :select "id, name, description, subtype"
                            :order  "name:asc,id:asc"}
                           #(dispatch [::set-infra-services %])]}))


(reg-event-db
  ::set-infra-registries-creds
  (fn [db [_ {creds :resources}]]
    (assoc db ::spec/infra-registries-creds creds
              ::spec/infra-registries-creds-loading? false)))


(reg-event-fx
  ::set-infra-registries
  (fn [{:keys [db]} [_ {infra-registries :resources}]]
    {:db                  (assoc db ::spec/infra-registries infra-registries
                                    ::spec/infra-registries-loading? false
                                    ::spec/infra-registries-creds nil
                                    ::spec/infra-registries-creds-loading? true)
     ::cimi-api-fx/search [:credential
                           {:filter (general-utils/join-and
                                      "subtype='infrastructure-service-registry'"
                                      (apply general-utils/join-or
                                             (map #(str "parent='" (:id %) "'") infra-registries))),
                            :select "id, parent, name, description"
                            :order  "name:asc,id:asc"}
                           #(dispatch [::set-infra-registries-creds %])]}))


(reg-event-fx
  ::get-infra-registries
  (fn [{:keys [db]} [_ registry-ids]]
    {:db                  (assoc db ::spec/infra-registries-loading? true
                                    ::spec/infra-registries)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter (general-utils/join-and
                                      "subtype='registry'"
                                      (apply general-utils/join-or
                                             (map #(str "id='" % "'") registry-ids))),
                            :select "id, name, description"
                            :order  "name:asc,id:asc"}
                           #(dispatch [::set-infra-registries %])]}))


(reg-event-db
  ::set-credential-registry
  (fn [{:keys [::spec/deployment] :as db} [_ index credential-id]]
    (let [private-registries   (get-in deployment [:module :content :private-registries])
          old-registries-creds (get deployment :registries-credentials
                                    (vec (take (count private-registries) (repeat nil))))
          registries-creds     (assoc old-registries-creds index credential-id)
          update-deployment    (assoc deployment :registries-credentials registries-creds)]
      (assoc db ::spec/deployment update-deployment))))


(reg-event-fx
  ::set-deployment
  (fn [{db :db} [_ deployment]]
    (let [registry-ids (get-in deployment [:module :content :private-registries])]
      (cond-> {:db (assoc db ::spec/deployment deployment
                             ::spec/loading-deployment? false)}
              (some? registry-ids) (assoc :dispatch [::get-infra-registries registry-ids])))))


(reg-event-fx
  ::get-deployment
  (fn [{:keys [db]} [_ id]]
    {:db               (assoc db ::spec/deployment {:id id})
     ::cimi-api-fx/get [id #(let [module-subtype (get-in % [:module :subtype])
                                  is-kubernetes? (= module-subtype "application_kubernetes")
                                  filter         (if is-kubernetes?
                                                   "subtype='kubernetes'"
                                                   "subtype='swarm'")]
                              (dispatch [::get-infra-services filter])
                              (dispatch [::set-deployment %]))]}))


(reg-event-fx
  ::create-deployment
  (fn [{{:keys [::spec/deployment] :as db} :db} [_ id first-step do-not-open-modal?]]
    (when (= :data first-step)
      (dispatch [::get-data-records]))
    (let [data              {:module {:href id}}
          old-deployment-id (:id deployment)
          on-success        #(dispatch [::get-deployment (:resource-id %)])]
      (cond->
        {:db               (assoc db ::spec/loading-deployment? true
                                     ::spec/deployment nil
                                     ::spec/selected-credential-id nil
                                     ::spec/deploy-modal-visible? (not (boolean do-not-open-modal?))
                                     ::spec/active-step (or first-step :data)
                                     ::spec/data-step-active? (= first-step :data)
                                     ::spec/cloud-filter nil
                                     ::spec/selected-cloud nil
                                     ::spec/cloud-infra-services nil
                                     ::spec/data-clouds nil)
         ::cimi-api-fx/add [:deployment data on-success]}
        old-deployment-id (assoc ::cimi-api-fx/delete [old-deployment-id #() :on-error #()])))))


(reg-event-fx
  ::get-credentials
  (fn [{db :db} _]
    {:db       (assoc db ::spec/credentials-loading? true
                         ::spec/credentials nil
                         ::spec/selected-credential-id nil)
     :dispatch [::refresh-credentials]}))


(reg-event-fx
  ::refresh-credentials
  (fn [{{:keys [::spec/selected-infra-service]} :db} _]
    (when-let [infra-id (:id selected-infra-service)]
      {::cimi-api-fx/search
       [:credential
        {:select "id, name, description, created, subtype, last-check, status"
         :filter (general-utils/join-and
                   (str "parent='" infra-id "'")
                   (str "subtype='infrastructure-service-swarm'"))}
        #(dispatch [::set-credentials %])]})))


(reg-event-fx
  ::start-deployment
  (fn [{:keys [db]} [_ id]]
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
                               (dispatch [::history-events/navigate "dashboard"]))))]
      {:db                     (assoc db ::spec/deploy-modal-visible? false)
       ::cimi-api-fx/operation [id "start" start-callback]})))


(reg-event-fx
  ::edit-deployment
  (fn [{{:keys [::spec/deployment]} :db} _]
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
      {::cimi-api-fx/edit [resource-id deployment edit-callback]})))


(reg-event-db
  ::set-cloud-infra-services
  (fn [db [_ {infra-services :resources}]]
    (assoc db ::spec/cloud-infra-services (into {} (map (juxt :id identity) infra-services)))))


(defn set-data-infra-service-and-filter
  [db infra-service]
  (assoc db ::spec/selected-cloud infra-service
            ::spec/cloud-filter (str "infrastructure-service='" infra-service "'")))


(reg-event-fx
  ::get-cloud-infra-services
  (fn [_ [_ filter]]
    {::cimi-api-fx/search [:infrastructure-service
                           {:filter filter, :select "id, name, description, subtype"}
                           #(dispatch [::set-cloud-infra-services %])]}))


(reg-event-fx
  ::set-data-infra-services
  (fn [{:keys [db]} [_ data-clouds-response]]
    (let [buckets        (get-in data-clouds-response
                                 [:aggregations (keyword "terms:infrastructure-service") :buckets])
          infra-services (map :key buckets)
          filter         (apply general-utils/join-or (map #(str "id='" % "'") infra-services))]

      {:db       (cond-> (assoc db ::spec/data-clouds buckets)
                         (= 1 (count infra-services)) (set-data-infra-service-and-filter
                                                        (first infra-services)))
       :dispatch [::get-cloud-infra-services filter]})))


(reg-event-fx
  ::get-data-records
  (fn [{{:keys [::data-spec/time-period-filter
                ::data-spec/content-type-filter] :as db} :db} _]
    (let [filter (general-utils/join-and time-period-filter content-type-filter)]
      {:db db
       ::cimi-api-fx/search
           [:data-record {:filter      filter
                          :last        0
                          :aggregation "terms:infrastructure-service"}
            #(dispatch [::set-data-infra-services %])]})))


(reg-event-db
  ::set-infra-service-filter
  (fn [db [_ cloud]]
    (set-data-infra-service-and-filter db cloud)))
