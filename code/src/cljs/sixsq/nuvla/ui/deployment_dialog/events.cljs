(ns sixsq.nuvla.ui.deployment-dialog.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.data-set.spec :as data-set-spec]
            [sixsq.nuvla.ui.data.spec :as data-spec]
            [sixsq.nuvla.ui.deployment-dialog.spec :as spec]
            [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.deployments-detail.events :as deployments-detail-events]
            [sixsq.nuvla.ui.i18n.spec :as i18n-spec]
            [sixsq.nuvla.ui.intercom.events :as intercom-events]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.general :as general-utils]
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
                ::spec/cloud-filter
                ::data-set-spec/time-period
                ::data-spec/content-type-filter
                ::data-set-spec/selected-data-record-ids]} :db}]
    (let [data-filter (if content-type-filter
                        {:records {:filters
                                   [{:filter     (general-utils/join-and cloud-filter content-type-filter)
                                     :data-type  "data-record"
                                     :time-start (first time-period)
                                     :time-end   (second time-period)}]}}
                        {:records {:records-ids selected-data-record-ids}})]
      {:dispatch [::set-deployment (assoc deployment :data data-filter)]})))

(reg-event-fx
  ::set-selected-credential
  (fn [{{:keys [::spec/deployment
                ::spec/data-step-active?] :as db} :db} [_ {:keys [id] :as _credential}]]
    {:db         (assoc db ::spec/selected-credential-id id
                           ::spec/deployment (assoc deployment :parent id))
     :dispatch-n [(when data-step-active? [::set-data-filters])]}))

(reg-event-db
  ::set-active-step
  (fn [db [_ active-step]]
    (assoc db ::spec/active-step active-step)))

(reg-event-db
  ::set-deploy-status
  (fn [db [_ step-id status]]
    (assoc-in db [::spec/step-states step-id :status] status)))

(reg-event-fx
  ::set-selected-infra-service
  (fn [{{:keys [::spec/deployment] :as db} :db} [_ infra-service]]
    {:db (assoc db ::spec/selected-infra-service infra-service
                   ::spec/credentials nil
                   ::spec/selected-credential-id nil)
     :fx [(when (utils/infra-app-compatible?
                  (:module deployment)
                  infra-service) [:dispatch [::get-credentials]])]}))

(reg-event-fx
  ::set-infra-services
  (fn [{db :db} [_ {infra-services :resources}]]
    {:db (assoc db ::spec/infra-services infra-services
                   ::spec/infra-services-loading? false)}))

(reg-event-fx
  ::get-infra-services
  (fn [{:keys [db]} [_ filter]]
    {:db                  (assoc db ::spec/infra-services-loading? true
                                    ::spec/infra-services nil)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter  filter,
                            :select  "id, name, description, subtype, capabilities, swarm-enabled, swarm-manager"
                            :orderby "name:asc,id:asc"}
                           #(dispatch [::set-infra-services %])]}))

(defn deployment-update-registries
  [deployment registries-creds-info]
  (let [registry-ids (get-in deployment [:module :content :private-registries])]
    (->> registry-ids
         (map #(-> registries-creds-info (get %) (get :cred-id "")))
         (assoc deployment :registries-credentials))))

(reg-event-fx
  ::set-infra-registries-creds
  (fn [{{:keys [::spec/deployment] :as db} :db} [_ registry-ids reg-creds-ids
                                                 infra-registries {creds :resources}]]
    (let [infra-registries-creds (group-by :parent creds)
          registries-creds-info  (->>
                                   registry-ids
                                   (map-indexed
                                     (fn [n reg-id]
                                       (let [cred-id (some-> (nth reg-creds-ids n)
                                                             str/trim
                                                             not-empty)]
                                         [reg-id
                                          {:cred-id      cred-id
                                           :preselected? (some? cred-id)}])))
                                   (into {}))

          single-cred-dispatch   (->> infra-registries-creds
                                      (filter #(= 1 (count (second %))))
                                      (mapv (fn [[infra-id [{cred-id :id}]]]
                                              [::set-credential-registry infra-id cred-id])))]
      (cond-> {:db (assoc db ::spec/infra-registries-creds infra-registries-creds
                             ::spec/infra-registries infra-registries
                             ::spec/infra-registries-loading? false
                             ::spec/registries-creds registries-creds-info
                             ::spec/deployment (deployment-update-registries
                                                 deployment registries-creds-info))}
              (seq single-cred-dispatch) (assoc :dispatch-n single-cred-dispatch)))))

(reg-event-fx
  ::set-infra-registries
  (fn [{:keys [db]} [_ registry-ids reg-creds-ids {infra-registries :resources}]]
    (cond-> {:db (assoc db ::spec/infra-registries-creds nil)}
            (seq infra-registries)
            (assoc ::cimi-api-fx/search
                   [:credential
                    {:filter  (general-utils/join-and
                                "subtype='infrastructure-service-registry'"
                                (apply general-utils/join-or
                                       (map #(str "parent='" (:id %) "'")
                                            infra-registries)))
                     :select  "id, parent, name, description, last-check, status, subtype"
                     :orderby "name:asc,id:asc"}
                    #(dispatch [::set-infra-registries-creds registry-ids
                                reg-creds-ids infra-registries %])])

            (empty? infra-registries)
            (assoc :dispatch [::set-infra-registries-creds
                              registry-ids reg-creds-ids infra-registries {:resources []}]))))

(reg-event-fx
  ::get-infra-registries
  (fn [{:keys [db]} [_ registry-ids reg-creds-ids]]
    {:db                  (assoc db ::spec/infra-registries-loading? true)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter  (general-utils/join-and
                                       "subtype='registry'"
                                       (apply general-utils/join-or
                                              (map #(str "id='" % "'") registry-ids))),
                            :select  "id, name, description"
                            :orderby "name:asc,id:asc"}
                           #(dispatch [::set-infra-registries registry-ids reg-creds-ids %])]}))

(reg-event-db
  ::set-credential-registry
  (fn [{:keys [::spec/registries-creds
               ::spec/infra-registries-creds
               ::spec/deployment] :as db} [_ infra-id cred-id]]
    (let [update-registries-creds (update registries-creds infra-id assoc :cred-id cred-id)]
      (assoc db ::spec/registries-creds update-registries-creds
                ::spec/deployment (deployment-update-registries
                                    deployment
                                    update-registries-creds)))))

(reg-event-fx
  ::set-deployment
  (fn [{db :db} [_ deployment]]
    (let [content      (get-in deployment [:module :content])
          registry-ids (:private-registries content)
          new-db       (assoc db ::spec/deployment deployment)]
      (if registry-ids
        {:db new-db
         :fx [[:dispatch
               [::get-infra-registries registry-ids
                (or
                  (:registries-credentials deployment)
                  (:registries-credentials content))]]]}
        {:db (assoc new-db ::spec/registries-creds nil)}))))

(reg-event-db
  ::set-check-dct-result
  (fn [{:keys [::spec/deployment] :as db} [_ {:keys [target-resource status-message] :as _job}]]
    (if (= (:href target-resource) (:id deployment))
      (let [result (if-let [dct (general-utils/json->edn status-message :keywordize-keys false)]
                     {:dct dct}
                     {:error (str "Error: " status-message)})]
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
    {:dispatch-later [{:ms 3000 :dispatch [::get-job-check-dct job-id]}]}))

(reg-event-fx
  ::check-dct
  (fn [_ [_ {:keys [id] :as _deployment}]]
    (let [on-success #(dispatch [::check-dct-later (:location %)])]
      {::cimi-api-fx/operation [id "check-dct" on-success]})))

; What's the difference with the same event in deployment
(reg-event-fx
  ::get-deployment
  (fn [{:keys [db]} [_ id]]
    {:db               (assoc db ::spec/deployment {:id id})
     ::cimi-api-fx/get [id #(let [{:keys [content subtype href]} (:module %)
                                  is-kubernetes? (= subtype "application_kubernetes")
                                  filter         (if is-kubernetes?
                                                   "subtype='kubernetes'"
                                                   "subtype='swarm'")]
                              (dispatch [::get-infra-services filter])
                              (dispatch [::set-deployment %])
                              (dispatch [::check-dct %])
                              (dispatch [::get-module-info href])
                              (dispatch [::set-selected-version (:id content)])
                              (dispatch [::set-original-module (:module %)]))]}))

(reg-event-fx
  ::create-deployment
  (fn [{{:keys [::spec/deployment] :as db} :db} [_ id first-step do-not-open-modal?]]
    (when (= :data first-step)
      (dispatch [::get-data-records]))
    (let [from-module?      (str/starts-with? id "module/")
          old-deployment-id (:id deployment)
          on-success        #(dispatch [::get-deployment (:resource-id %)])
          on-error          #(do
                               (dispatch [::reset])
                               (dispatch
                                 [::messages-events/add
                                  (let [{:keys [status message]} (response/parse-ex-info %)]
                                    {:header  (cond-> "Error during creation of deployment"
                                                      status (str " (" status ")"))
                                     :content message
                                     :type    :error})]))]
      (cond->
        {:db (assoc db ::spec/deployment nil
                       ::spec/selected-credential-id nil
                       ::spec/selected-infra-service nil
                       ::spec/deploy-modal-visible? (not (boolean do-not-open-modal?))
                       ::spec/active-step (or first-step :data)
                       ::spec/data-step-active? (= first-step :data)
                       ::spec/cloud-filter nil
                       ::spec/selected-cloud nil
                       ::spec/cloud-infra-services nil
                       ::spec/data-clouds nil
                       ::spec/license-accepted? false
                       ::spec/module-info nil
                       ::spec/selected-version nil
                       ::spec/original-module nil)}
        from-module? (assoc ::cimi-api-fx/add [:deployment {:module {:href id}} on-success :on-error on-error])
        (not from-module?) (assoc ::cimi-api-fx/operation [id "clone" on-success :on-error on-error])
        old-deployment-id (assoc ::cimi-api-fx/delete [old-deployment-id #() :on-error #()])))))

(defn on-error-reselect-credential
  [tr error-content resp]
  (dispatch [::reset])
  (dispatch [::messages-events/add
             {:header  (tr [:destination-not-found])
              :content (str error-content "\n" (response/parse-ex-info resp))
              :type    :error}]))

(reg-event-fx
  ::reselect-credential
  (fn [{{:keys [::i18n-spec/tr]} :db} [_ credential]]
    (let [error-content (tr [:deployment-update-failed-infra-retrieval-fail])
          on-error      (partial on-error-reselect-credential tr error-content)]
      (when credential
        {::cimi-api-fx/get [(:parent credential)
                            #(dispatch [::set-selected-infra-service %])
                            :on-error on-error]}))))

(reg-event-fx
  ::open-deployment-modal
  (fn [{{:keys [::i18n-spec/tr] :as db} :db} [_ first-step
                                              {:keys [parent id]
                                               :as   _deployment}]]
    (let [error-content (tr [:deployment-update-failed-cred-retrieval-fail])
          on-error      (partial on-error-reselect-credential tr error-content)]
      (cond->
        {:db (assoc db ::spec/deployment nil
                       ::spec/selected-credential-id nil
                       ::spec/selected-infra-service nil
                       ::spec/deploy-modal-visible? true
                       ::spec/active-step (or first-step :data)
                       ::spec/data-step-active? (= first-step :data)
                       ::spec/cloud-filter nil
                       ::spec/selected-cloud nil
                       ::spec/cloud-infra-services nil
                       ::spec/data-clouds nil
                       ::spec/registries-creds nil
                       ::spec/module-info nil
                       ::spec/selected-version nil
                       ::spec/original-module nil)
         :fx [[:dispatch [::get-deployment id]]
              (when (= :data first-step)
                [:dispatch [::get-data-records]])]}
        parent (assoc ::cimi-api-fx/get
                      [parent #(dispatch [::reselect-credential %])
                       :on-error on-error])))))

(reg-event-fx
  ::get-credentials
  (fn [{db :db}]
    {:db       (assoc db ::spec/credentials-loading? true)
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

(reg-event-db
  ::set-submit-loading?
  (fn [db [_ loading?]]
    (assoc db ::spec/submit-loading? loading?)))

(reg-event-fx
  ::deployment-operation
  (fn [_ [_ id operation]]
    (let [on-success (fn [response]
                       (let [{:keys [message]} (response/parse response)
                             success-msg {:header  (str operation " action called successfully")
                                          :content message
                                          :type    :success}]
                         (dispatch [::reset])
                         (dispatch [::messages-events/add success-msg])
                         (dispatch [::deployments-detail-events/get-deployment id])
                         (dispatch [::routing-events/navigate
                                    routes/deployment-details {:uuid (general-utils/id->uuid id)}])))
          on-error   (fn [response]
                       (dispatch [::set-error-message
                                  (str "Error occured during \"" operation
                                       "\" action on deployment")
                                  (-> response response/parse-ex-info :message)])
                       (dispatch [::set-submit-loading? false]))]
      {::cimi-api-fx/operation [id operation on-success :on-error on-error]})))

(reg-event-fx
  ::edit-deployment
  (fn [{{:keys [::spec/deployment] :as db} :db} [_ operation]]
    (let [resource-id   (:id deployment)
          operation     (or operation "start")
          dep           (dissoc deployment :execution-mode)
          edit-callback (fn [response]
                          (if (instance? js/Error response)
                            (do
                              (dispatch [::set-error-message
                                         "Error during edition of deployment"
                                         (-> response response/parse-ex-info :message)])
                              (dispatch [::set-submit-loading? false]))
                            (do
                              (dispatch [::deployment-operation resource-id operation])
                              (dispatch [::intercom-events/set-event "Last app launch"
                                         (time/timestamp)]))))]
      {:db                (assoc db ::spec/submit-loading? true)
       ::cimi-api-fx/edit [resource-id dep edit-callback]})))

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
  (fn [{{:keys [::data-set-spec/time-period-filter
                ::data-spec/content-type-filter
                ::data-set-spec/selected-data-record-ids] :as db} :db} _]
    (let [filter (if (nil? content-type-filter)
                   (apply general-utils/join-or (map #(str "id='" % "'") selected-data-record-ids))
                   (general-utils/join-and time-period-filter content-type-filter))]
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

(reg-event-db
  ::set-selected-version
  (fn [db [_ version]]
    (assoc db ::spec/selected-version version)))

(reg-event-db
  ::set-original-module
  (fn [db [_ module]]
    (assoc db ::spec/original-module module)))

(reg-event-db
  ::set-module-info
  (fn [db [_ module]]
    (assoc db ::spec/module-info module)))

(reg-event-fx
  ::get-module-info
  (fn [_ [_ module-id]]
    {::cimi-api-fx/get [module-id #(dispatch [::set-module-info %])]}))

(reg-event-fx
  ::fetch-module
  (fn [{{:keys [::spec/deployment]} :db} [_ module-version-href]]
    {::cimi-api-fx/get
     [module-version-href
      #(dispatch [::set-deployment (update deployment :module utils/merge-module %)])]}))
