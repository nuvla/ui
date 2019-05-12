(ns sixsq.nuvla.ui.infra-service.events
  (:require
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.infra-service.spec :as spec]
    [sixsq.nuvla.ui.infra-service.utils :as utils]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [cljs.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.response :as response]))


; Perform form validation if validate-form? is true.

(reg-event-db
  ::validate-swarm-service-form
  (fn [db [_]]
    (let [form-spec      ::spec/swarm-service
          service        (get db ::spec/service)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form? (if (nil? form-spec) true (s/valid? form-spec service)) true)]
      (s/explain form-spec service)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-db
  ::validate-minio-service-form
  (fn [db [_]]
    (let [form-spec      ::spec/minio-service
          service        (get db ::spec/service)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form? (if (nil? form-spec) true (s/valid? form-spec service)) true)]
      (s/explain form-spec service)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-db
  ::set-service
  (fn [db [_ service]]
    (assoc db ::spec/service service)))


(reg-event-db
  ::set-services
  (fn [db [_ data]]
    (let [services (:resources data)
          groups   (group-by :parent services)]
      (-> db
          (assoc-in [::spec/services :groups] groups)
          (assoc-in [::spec/services :count] (get data :count 0))))))


(reg-event-db
  ::set-service-group
  (fn [db [_ group services]]
    (-> db
        (assoc-in [::spec/service :parent] (:id group))
        (assoc-in [::spec/service-group :services] services))))


(reg-event-db
  ::reset-service-group
  (fn [db [_]]
    (-> db
        (dissoc ::spec/service-group))))


(reg-event-fx
  ::add-service-group
  (fn [{{:keys [::client-spec/client] :as db} :db :as cofx} [_]]
    (let [new-group (utils/db->new-service-group db)]
      {:db               db
       ::cimi-api-fx/add [client "infrastructure-service-group" new-group
                          #(if (instance? js/Error %)
                             (let [{:keys [status message]} (response/parse-ex-info %)]
                               (dispatch [::messages-events/add
                                          {:header  (cond-> (str "error creating new group")
                                                            status (str " (" status ")"))
                                           :content message
                                           :type    :error}]))
                             (do (dispatch [::cimi-detail-events/get (:id %)])
                                 (dispatch [::set-service-group (:id %)])
                                 (dispatch [::close-service-modal])
                                 (dispatch [::get-services])))]})))


(reg-event-fx
  ::edit-service
  (fn [{{:keys [::spec/swarm-service ::client-spec/client] :as db} :db :as cofx} [_]]
    (let [service     swarm-service
          id          (:id service)
          new-service (utils/db->new-swarm-service db)]
      (if (nil? id)
        (do
          (dispatch [::add-service-group])
          {:db               db
           ::cimi-api-fx/add [client "service" new-service
                              #(if (instance? js/Error %)
                                 (let [{:keys [status message]} (response/parse-ex-info %)]
                                   (dispatch [::messages-events/add
                                              {:header  (cond-> (str "error editing")
                                                                status (str " (" status ")"))
                                               :content message
                                               :type    :error}]))
                                 (do (dispatch [::cimi-detail-events/get (:id %)])
                                     (dispatch [::close-service-modal])
                                     (dispatch [::get-services])))]})
        {:db                db
         ::cimi-api-fx/edit [client id service
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::cimi-detail-events/get (:id %)])
                                    (dispatch [::close-service-modal])
                                    (dispatch [::get-services])))]}))))


(reg-event-fx
  ::delete-service
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ id]]
    {:db                  db
     ::cimi-api-fx/delete [client id
                           #(if (instance? js/Error %)
                              (let [{:keys [status message]} (response/parse-ex-info %)]
                                (dispatch [::messages-events/add
                                           {:header  (cond-> (str "error deleting service " id)
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :error}]))
                              (dispatch [::get-services]))]}))


(reg-event-db
  ::open-service-modal
  (fn [db [_ service is-new?]]
    (-> db
        (assoc ::spec/service service)
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
  ::get-services
  (fn [{{:keys [::client-spec/client
                ::spec/full-text-search
                ::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    (when client
      {:db                  (assoc db ::spec/services nil)
       ::cimi-api-fx/search [client :infrastructure-service (utils/get-query-params full-text-search page elements-per-page)
                             #(dispatch [::set-services %])]})))


(reg-event-fx
  ::set-full-text-search
  (fn [{{:keys [::client-spec/client
                ::spec/elements-per-page] :as db} :db} [_ full-text-search]]
    (let [new-page 1]
      {:db                  (assoc db ::spec/full-text-search full-text-search
                                      ::spec/page new-page)
       ::cimi-api-fx/search [client :infrastructure-service (utils/get-query-params full-text-search new-page elements-per-page)
                             #(dispatch [::set-services %])]})))


(reg-event-fx
  ::set-page
  (fn [{{:keys [::client-spec/client
                ::spec/full-text-search
                ::spec/page
                ::spec/elements-per-page] :as db} :db} [_ page]]
    {:db                  (assoc db ::spec/page page)
     ::cimi-api-fx/search [client :infrastructure-service (utils/get-query-params full-text-search page elements-per-page)
                           #(dispatch [::set-services %])]}))


(reg-event-db
  ::set-validate-form?
  (fn [db [_ validate-form?]]
    (assoc db ::spec/validate-form? validate-form?)))


(reg-event-db
  ::active-input
  (fn [db [_ input-name]]
    (assoc db ::spec/active-input input-name)))


(reg-event-db
  ::form-invalid
  (fn [db [_]]
    (assoc db ::spec/form-valid? false)))


(reg-event-db
  ::form-valid
  (fn [db [_]]
    (assoc db ::spec/form-valid? true)))


(reg-event-db
  ::update-service
  (fn [db [_ key value]]
    (assoc-in db [::spec/service key] value)))
