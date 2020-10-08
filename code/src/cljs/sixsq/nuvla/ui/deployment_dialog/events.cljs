(ns sixsq.nuvla.ui.deployment-dialog.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch inject-cofx reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.credentials.events :as creds-events]
    [sixsq.nuvla.ui.data.spec :as data-spec]
    [sixsq.nuvla.ui.deployment-dialog.spec :as spec]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.intercom.events :as intercom-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.general :as utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [sixsq.nuvla.ui.utils.time :as time]))


(reg-event-fx
  ::reset
  (fn [{db :db} _]
    {:db (merge db spec/defaults)}))


(reg-event-fx
  ::delete-deployment
  (fn [_ [_ id]]
    {::cimi-api-fx/delete [id #()]}))


(reg-event-fx
  ::set-credentials
  (fn [{{:keys [::spec/selected-credential-id
                ::spec/deployment] :as db} :db} [_ {credentials :resources}]]
    (let [only-one-cred (= (count credentials) 1)
          first-cred    (when only-one-cred (first credentials))
          dep-cred-id   (:parent deployment)
          selected-cred (or
                          (when (and (nil? selected-credential-id)
                                     dep-cred-id)
                            (some #(when (= dep-cred-id (:id %)) %) credentials))
                          (when (and only-one-cred
                                     (not= selected-credential-id (:id first-cred)))
                            first-cred))]
      (cond-> {:db (assoc db ::spec/credentials credentials
                             ::spec/credentials-loading? false)}
              selected-cred (assoc :dispatch [::set-selected-credential selected-cred])))))


(reg-event-fx
  ::set-data-filters
  (fn [{{:keys [::spec/deployment
                ::data-spec/time-period
                ::data-spec/full-text-search
                ::spec/cloud-filter
                ::data-spec/content-type-filter] :as db} :db} _]
    (let [filter       (general-utils/join-and cloud-filter
                                               content-type-filter
                                               full-text-search)
          data-filters {:records {:filters
                                  [{:filter     filter
                                    :data-type  "data-record"
                                    :time-start (first time-period)
                                    :time-end   (second time-period)}]}}]
      {:dispatch [::set-deployment (assoc deployment :data data-filters)]})))


(reg-event-fx
  ::set-selected-credential
  (fn [{{:keys [::spec/deployment
                ::spec/data-step-active?] :as db} :db} [_ {:keys [id] :as credential}]]
    {:db         (assoc db ::spec/selected-credential-id id
                           ::spec/deployment (assoc deployment :parent id))
     :dispatch-n [(when data-step-active? [::set-data-filters])
                  [::creds-events/check-credential credential 5]]}))


(reg-event-db
  ::set-active-step
  (fn [db [_ active-step]]
    (assoc db ::spec/active-step active-step)))


(reg-event-fx
  ::set-selected-infra-service
  (fn [{:keys [db]} [_ infra-service]]
    {:db       (assoc db ::spec/selected-infra-service infra-service)
     :dispatch [::get-credentials]}))


(reg-event-fx
  ::set-infra-services
  (fn [{{:keys [::spec/selected-infra-service] :as db} :db} [_ {infra-services :resources}]]
    (let [first-infra (when-not selected-infra-service (first infra-services))]
      (cond-> {:db (assoc db ::spec/infra-services infra-services
                             ::spec/infra-services-loading? false)}
              first-infra (assoc :dispatch [::set-selected-infra-service first-infra])))))


(reg-event-fx
  ::get-infra-services
  (fn [{:keys [db]} [_ filter]]
    {:db                  (assoc db ::spec/infra-services-loading? true
                                    ::spec/infra-services nil)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter filter,
                            :select "id, name, description, subtype"
                            :order  "name:asc,id:asc"}
                           #(dispatch [::set-infra-services %])]}))


(reg-event-fx
  ::set-infra-registries-creds
  (fn [{{:keys [::spec/deployment] :as db} :db} [_ {creds :resources}]]
    (let [infra-registries-creds     (group-by :parent creds)
          pre-selected-cred-ids-set  (-> deployment
                                         (get-in [:module :content :registries-credentials])
                                         set)
          single-cred-dispatch       (->> infra-registries-creds
                                          (filter #(= 1 (count (second %))))
                                          (mapv (fn [[infra-id [{cred-id :id}]]]
                                                  [::set-credential-registry infra-id cred-id])))
          pre-selected-cred-dispatch (->> creds
                                          (filter #(contains? pre-selected-cred-ids-set (:id %)))
                                          (mapv (fn [{infra-id :parent cred-id :id}]
                                                  [::set-credential-registry infra-id cred-id])))
          select-cred-dispatch       (concat single-cred-dispatch pre-selected-cred-dispatch)]
      (cond-> {:db (assoc db ::spec/infra-registries-creds infra-registries-creds
                             ::spec/infra-registries-creds-loading? false)}
              (seq select-cred-dispatch) (assoc :dispatch-n select-cred-dispatch)))))


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
                                             (map #(str "parent='" (:id %) "'") infra-registries)))
                            :select "id, parent, name, description, last-check, status, subtype"
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


(reg-event-fx
  ::set-credential-registry
  (fn [{{:keys [::spec/registries-creds
                ::spec/infra-registries-creds
                ::spec/deployment] :as db} :db} [_ infra-id cred-id]]
    (let [update-registries-creds (assoc registries-creds infra-id cred-id)
          registries-credentials  (->> update-registries-creds vals (remove nil?))
          credential              (-> infra-registries-creds (get infra-id) first)]
      {:db       (assoc db ::spec/registries-creds update-registries-creds
                           ::spec/deployment (assoc deployment
                                               :registries-credentials registries-credentials))
       :dispatch [::creds-events/check-credential credential 5]})))


(reg-event-db
  ::set-deployment
  (fn [db [_ deployment]]
    (assoc db ::spec/deployment deployment
              ::spec/loading-deployment? false)))


(reg-event-db
  ::set-check-dct-result
  (fn [{:keys [::spec/deployment] :as db} [_ {:keys [target-resource status-message] :as job}]]
    (if (= (:href target-resource) (:id deployment))
      (let [result (try
                     {:dct (utils/json->edn status-message :keywordize-keys false)}
                     (catch :default _
                       {:error (str "Error: " status-message)}))]
        (assoc db ::spec/check-dct result))
      db)))


(reg-event-fx
  ::set-job-check-dct
  (fn [_ [_ {:keys [id state] :as job}]]
    (let [job-finished? (-> state
                            (#{"STOPPED" "SUCCESS" "FAILED"})
                            boolean)]
      (if job-finished?
        (dispatch [::set-check-dct-result job])
        (dispatch [::check-dct-later id])))))



(reg-event-fx
  ::get-job-check-dct
  (fn [_ [_ job-id]]
    {::cimi-api-fx/get [job-id #(dispatch [::set-job-check-dct %])]}))


(reg-event-fx
  ::check-dct-later
  (fn [_ [_ job-id]]
    {:dispatch-later
     [{:ms 3000 :dispatch [::get-job-check-dct job-id]}]}))


(reg-event-fx
  ::check-dct
  (fn [_ [_ {:keys [id] :as deployment}]]
    {::cimi-api-fx/operation [id "check-dct" #(dispatch [::check-dct-later (:location %)])]}))


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
                              (dispatch [::set-deployment %])
                              (dispatch [::check-dct %])
                              (when-let [registry-ids (some-> %
                                                              :module
                                                              :content
                                                              :private-registries)]
                                (dispatch [::get-infra-registries registry-ids]))
                              )]}))


(reg-event-fx
  ::create-deployment
  (fn [{{:keys [::spec/deployment] :as db} :db} [_ id first-step do-not-open-modal?]]
    (when (= :data first-step)
      (dispatch [::get-data-records]))
    (let [data              (if (str/starts-with? id "module/")
                              {:module {:href id}}
                              {:deployment {:href id}})
          old-deployment-id (:id deployment)
          on-success        #(dispatch [::get-deployment (:resource-id %)])]
      (cond->
        {:db               (assoc db ::spec/loading-deployment? true
                                     ::spec/deployment nil
                                     ::spec/selected-credential-id nil
                                     ::spec/selected-infra-service nil
                                     ::spec/deploy-modal-visible? (not (boolean do-not-open-modal?))
                                     ::spec/active-step (or first-step :data)
                                     ::spec/data-step-active? (= first-step :data)
                                     ::spec/cloud-filter nil
                                     ::spec/selected-cloud nil
                                     ::spec/cloud-infra-services nil
                                     ::spec/data-clouds nil
                                     ::spec/license-accepted? false)
         ::cimi-api-fx/add [:deployment data on-success
                            :on-error #(do
                                         (dispatch [::reset])
                                         (dispatch
                                           [::messages-events/add
                                            (let [{:keys [status message]} (response/parse-ex-info %)]
                                              {:header  (cond-> "Error during creation of deployment"
                                                                status (str " (" status ")"))
                                               :content message
                                               :type    :error})]))]}
        old-deployment-id (assoc ::cimi-api-fx/delete [old-deployment-id #() :on-error #()])))))


(reg-event-fx
  ::reselect-credential
  (fn [_ [_ credential]]
    (when credential
      {::cimi-api-fx/get [(:parent credential) #(dispatch [::set-selected-infra-service %])]}
      )))

(reg-event-fx
  ::open-deployment-modal
  (fn [{db :db} [_ first-step {:keys [parent id] :as deployment}]]
    (when (= :data first-step)
      (dispatch [::get-data-records]))
    (cond-> {:db       (assoc db ::spec/loading-deployment? true
                                 ::spec/deployment nil
                                 ::spec/selected-credential-id nil
                                 ::spec/selected-infra-service nil
                                 ::spec/deploy-modal-visible? true
                                 ::spec/active-step (or first-step :data)
                                 ::spec/data-step-active? (= first-step :data)
                                 ::spec/cloud-filter nil
                                 ::spec/selected-cloud nil
                                 ::spec/cloud-infra-services nil
                                 ::spec/data-clouds nil)
             :dispatch [::get-deployment id]}
            parent (assoc ::cimi-api-fx/get [parent #(dispatch [::reselect-credential %])]))))


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
                   (str "subtype='infrastructure-service-" (:subtype selected-infra-service) "'"))}
        #(dispatch [::set-credentials %])]})))


(reg-event-db
  ::set-error-message
  (fn [db [_ title content]]
    (assoc db ::spec/error-message {:title   title
                                    :content content})))


(reg-event-fx
  ::start-deployment
  (fn [_ [_ id]]
    (let [start-callback (fn [response]
                           (if (instance? js/Error response)
                             (dispatch [::set-error-message
                                        "Error while starting the deployment"
                                        (-> response response/parse-ex-info :message)])
                             (let [{:keys [status message resource-id]} (response/parse response)
                                   success-msg {:header  (cond-> (str "started " resource-id)
                                                                 status (str " (" status ")"))
                                                :content message
                                                :type    :success}]
                               (dispatch [::reset])
                               (dispatch [::messages-events/add success-msg])
                               (dispatch [:sixsq.nuvla.ui.dashboard-detail.events/get-deployment id])
                               (dispatch [::history-events/navigate
                                          (str "dashboard/" (general-utils/id->uuid id))]))))]
      {::cimi-api-fx/operation [id "start" start-callback]})))


(reg-event-fx
  ::edit-deployment
  (fn [{{:keys [::spec/deployment] :as db} :db} _]
    (let [resource-id   (:id deployment)
          edit-callback (fn [response]
                          (if (instance? js/Error response)
                            (dispatch [::set-error-message
                                       "Error during edition of deployment"
                                       (-> response response/parse-ex-info :message)])
                            (do
                              (dispatch [::start-deployment resource-id])
                              (dispatch [::intercom-events/set-event "Last app launch" (time/timestamp)]))))]
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


(reg-event-db
  ::set-license-accepted?
  (fn [db [_ accepted?]]
    (assoc db ::spec/license-accepted? accepted?)))


(reg-event-db
  ::set-price-accepted?
  (fn [db [_ accepted?]]
    (assoc db ::spec/price-accepted? accepted?)))
