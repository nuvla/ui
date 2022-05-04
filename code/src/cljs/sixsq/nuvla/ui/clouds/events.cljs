(ns sixsq.nuvla.ui.clouds.events
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
    [sixsq.nuvla.ui.clouds.spec :as spec]
    [sixsq.nuvla.ui.clouds.utils :as utils]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]))


; Perform form validation if validate-form? is true.

(defn map-update-if-empty
  [coll params]
  (reduce (fn [ncoll [k v]] (assoc ncoll k (if-not (get coll k nil) v (get coll k))))
          coll
          params))

(reg-event-db
  ::validate-coe-service-form
  (fn [db [_]]
    (let [coe-form-spec     ::spec/coe-service
          generic-form-spec ::spec/generic-service
          mgmt-cred-subtype (utils/mgmt-cred-subtype-by-id db (get-in db [::spec/infra-service :management-credential]))
          cloud-params      (get utils/cloud-params-defaults mgmt-cred-subtype)
          service           (map-update-if-empty (get db ::spec/infra-service) cloud-params)
          validate-form?    (get db ::spec/validate-form?)
          valid?            (if validate-form?
                              (if (or (nil? coe-form-spec) (nil? generic-form-spec))
                                true
                                ; :endpoint distinguishes pre-existing from to be deployed COE
                                (if (:endpoint service)
                                  (s/valid? generic-form-spec service)
                                  (s/valid? coe-form-spec service)))
                              true)]
      (s/explain coe-form-spec service)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-db
  ::validate-registry-service-form
  (fn [db [_]]
    (let [form-spec      ::spec/registry-service
          service        (get db ::spec/infra-service)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form?
                           (if (nil? form-spec) true (s/valid? form-spec service)) true)]
      (s/explain form-spec service)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-db
  ::validate-minio-service-form
  (fn [db [_]]
    (let [form-spec      ::spec/minio-service
          service        (get db ::spec/infra-service)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form?
                           (if (nil? form-spec) true (s/valid? form-spec service)) true)]
      (s/explain form-spec service)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-db
  ::set-infra-service
  (fn [db [_ service]]
    (assoc db ::spec/infra-service service)))


(reg-event-db
  ::set-infra-services
  (fn [db [_ data]]
    (let [services (:resources data)
          groups   (group-by :parent services)]
      (-> db
          (assoc-in [::spec/infra-services :groups] groups)
          (assoc ::main-spec/loading? false)))))


(reg-event-db
  ::set-service-group
  (fn [db [_ group services]]
    (-> db
        (assoc-in [::spec/infra-service :parent] (:id group))
        (assoc-in [::spec/service-group :services] services))))


(reg-event-db
  ::reset-service-group
  (fn [db [_]]
    (dissoc db ::spec/service-group)))

(reg-event-db
  ::reset-infra-service
  (fn [db [_]]
    (assoc db ::spec/infra-service {})))


(reg-event-db
  ::update-credential
  (fn [db [_ key value]]
    (assoc-in db [::spec/management-credential key] value)))


(reg-event-fx
  ::set-coe-management-credentials-available
  (fn [{db :db} [_ response]]
    (let [mgmt-creds (:resources response)]
      (cond-> {:db (assoc db ::spec/management-credentials-available mgmt-creds)}
              (= (:count response) 1) (assoc :dispatch
                                             [::update-credential :parent
                                              (-> mgmt-creds first :id)])))))


(reg-event-fx
  ::fetch-coe-management-credentials-available
  (fn [{:keys [db]} [_ subtypes additional-filter]]
    {:db                  (assoc db ::spec/management-credentials-available nil)
     ::cimi-api-fx/search [:credential
                           {:filter (cond-> (apply general-utils/join-or
                                                   (map #(str "subtype='" % "'") subtypes))
                                            additional-filter (general-utils/join-and additional-filter))
                            :last   10000}
                           #(dispatch [::set-coe-management-credentials-available %])]}))


(reg-event-fx
  ::add-infra-service
  (fn [{:keys [db]} [_ infra-service-group-id]]
    (let [new-service (-> db
                          (utils/db->new-service)
                          (assoc-in [:template :parent] infra-service-group-id))]
      {::cimi-api-fx/add [:infrastructure-service new-service
                          #(do (dispatch [::cimi-detail-events/get (:resource-id %)])
                               (dispatch [::close-service-modal])
                               (dispatch [::get-infra-service-groups]))]})))

(reg-event-fx
  ::edit-infra-service
  (fn [{{:keys [::spec/infra-service] :as db} :db} _]
    (let [{:keys [id, parent]} infra-service]
      (if id
        {::cimi-api-fx/edit [id infra-service
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::cimi-detail-events/get (:id %)])
                                    (dispatch [::close-service-modal])
                                    (dispatch [::get-infra-services])))]}
        (if parent
          (dispatch [::add-infra-service parent])
          (let [new-group (utils/db->new-service-group db)]
            {::cimi-api-fx/add [:infrastructure-service-group new-group
                                #(do (dispatch [::cimi-detail-events/get (:resource-id %)])
                                     (dispatch [::set-service-group (:resource-id %)])
                                     (dispatch [::close-service-modal])
                                     (dispatch [::get-infra-service-groups])
                                     (dispatch [::add-infra-service (:resource-id %)]))]}))))))


(reg-event-db
  ::open-service-modal
  (fn [db [_ service is-new?]]
    (-> db
        (assoc ::spec/infra-service service)
        (assoc ::spec/service-modal-visible? true)
        (assoc ::spec/is-new? is-new?))))


(reg-event-db
  ::close-service-modal
  (fn [db [_ _service _is-new?]]
    (-> db
        (assoc ::spec/service-modal-visible? false))))


(reg-event-db
  ::open-add-service-modal
  (fn [db [_]]
    (assoc db ::spec/add-service-modal-visible? true)))


(reg-event-db
  ::close-add-service-modal
  (fn [db [_]]
    (assoc db ::spec/add-service-modal-visible? false)))


(reg-event-fx
  ::get-infra-service-groups
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    {:db                  (assoc db ::spec/infra-service-groups nil)
     ::cimi-api-fx/search [:infrastructure-service-group
                           (utils/get-query-params page elements-per-page "parent=null")
                           #(dispatch [::set-infra-service-groups %])]}))


(reg-event-fx
  ::set-infra-service-groups
  (fn [{db :db} [_ {:keys [count resources]}]]
    (let [infra-service-groups (map #(select-keys % [:id :name]) resources)
          ids                  (map :id infra-service-groups)
          filter-services      (apply general-utils/join-or (map #(str "parent='" % "'") ids))]
      {:db                  (assoc db ::spec/infra-service-groups {:resources infra-service-groups
                                                                   :count     count})
       ::cimi-api-fx/search [:infrastructure-service
                             {:filter filter-services}
                             #(dispatch [::set-infra-services %])]})))


(reg-event-fx
  ::get-infra-services
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    {:db                  (assoc db ::spec/infra-services nil)
     ::cimi-api-fx/search [:infrastructure-service
                           (utils/get-query-params page elements-per-page)
                           #(dispatch [::set-infra-services %])]}))


(reg-event-fx
  ::set-page
  (fn [{{:keys [::spec/elements-per-page] :as db} :db} [_ page]]
    {:db (assoc db ::spec/page page)
     :fx [[:dispatch ::get-infra-service-groups]]}))


(reg-event-db
  ::set-validate-form?
  (fn [db [_ validate-form?]]
    (assoc db ::spec/validate-form? validate-form?)))


(reg-event-db
  ::form-valid
  (fn [db [_]]
    (assoc db ::spec/form-valid? true)))


(reg-event-db
  ::update-infra-service
  (fn [db [_ key value]]
    (assoc-in db [::spec/infra-service key] value)))


(reg-event-db
  ::update-infra-service-map
  (fn [db [_ kvs]]
    (update-in db [::spec/infra-service] merge kvs)))


(reg-event-db
  ::clear-infra-service-cloud-params
  (fn [db [_]]
    (update-in db [::spec/infra-service]
               (fn [nested] (apply dissoc nested utils/cloud-params-keys)))))


;; SSH keys

(reg-event-db
  ::ssh-keys
  (fn [db [_event-type ssh-keys]]
    (assoc-in db [::spec/infra-service :ssh-keys] ssh-keys)))


(reg-event-db
  ::set-ssh-keys-infra
  (fn [db [_ {resources :resources}]]
    (assoc db ::spec/ssh-keys-infra resources)))


(reg-event-fx
  ::get-ssh-keys-infra
  (fn [_ _]
    {::cimi-api-fx/search
     [:credential
      {:filter "subtype='ssh-key'"
       :select "id, name"
       :order  "name:asc, id:asc"
       :last   10000} #(dispatch [::set-ssh-keys-infra %])]}))

