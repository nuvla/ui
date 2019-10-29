(ns sixsq.nuvla.ui.infrastructures.events
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
    [sixsq.nuvla.ui.infrastructures.spec :as spec]
    [sixsq.nuvla.ui.infrastructures.utils :as utils]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


; Perform form validation if validate-form? is true.

(reg-event-db
  ::validate-swarm-service-form
  (fn [db [_]]
    (let [form-spec      ::spec/swarm-service
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
      (assoc-in db [::spec/infra-services :groups] groups))))


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


(reg-event-fx
  ::add-infra-service
  (fn [{:keys [db]} [_ infra-service-group-id]]
    (let [new-service (-> db
                          (utils/db->new-service)
                          (assoc-in [:template :parent] infra-service-group-id))]
      {::cimi-api-fx/add [:infrastructure-service new-service
                          #(do (dispatch [::cimi-detail-events/get (:resource-id %)])
                               (dispatch [::close-service-modal])
                               (dispatch [::get-infra-service-groups])
                               (dispatch [::main-events/check-bootstrap-message]))]})))

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
  (fn [db [_ service is-new?]]
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
                           (utils/get-query-params page elements-per-page)
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
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page] :as db} :db} [_ page]]
    {:db                  (assoc db ::spec/page page)
     ::cimi-api-fx/search [:infrastructure-service-group
                           (utils/get-query-params page elements-per-page)
                           #(dispatch [::set-infra-service-groups %])]}))


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
