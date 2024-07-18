(ns sixsq.nuvla.ui.pages.clouds.events
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.messages.events :as messages-events]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.pages.cimi-detail.events :as cimi-detail-events]
            [sixsq.nuvla.ui.pages.clouds.spec :as spec]
            [sixsq.nuvla.ui.pages.clouds.utils :as utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-db
  ::validate-coe-service-form
  (fn [db]
    (let [generic-form-spec ::spec/generic-service
          service           (get db ::spec/infra-service)
          validate-form?    (get db ::spec/validate-form?)
          valid?            (if validate-form?
                              (s/valid? generic-form-spec service)
                              true)]
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
  (fn [db]
    (dissoc db ::spec/service-group)))

(reg-event-db
  ::reset-infra-service
  (fn [db]
    (assoc db ::spec/infra-service {})))

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
  (fn [{{:keys [::spec/infra-service] :as db} :db}]
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
                                    (dispatch [::get-infra-service-groups])))]}
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
  (fn [{db :db}]
    {:db                  (assoc db ::spec/infra-service-groups nil)
     ::cimi-api-fx/search [:infrastructure-service-group
                           (->> {:select  "id, name"
                                 :orderby "created:desc"
                                 :filter  "parent=null"}
                                (pagination-plugin/first-last-params
                                  db [::spec/pagination]))
                           #(dispatch [::set-infra-service-groups %])]}))

(reg-event-fx
  ::set-infra-service-groups
  (fn [{db :db} [_ {:keys [count resources]}]]
    (let [infra-service-groups (map #(select-keys % [:id :name]) resources)]
      {:db                  (assoc db ::spec/infra-service-groups
                                      {:resources infra-service-groups
                                       :count     count})
       ::cimi-api-fx/search [:infrastructure-service
                             {:last   10000
                              :filter (general-utils/filter-eq-parent-vals
                                        (mapv :id infra-service-groups))}
                             #(dispatch [::set-infra-services %])]})))

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
