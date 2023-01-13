(ns sixsq.nuvla.ui.credentials.events
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
            [sixsq.nuvla.ui.credentials.spec :as spec]
            [sixsq.nuvla.ui.credentials.utils :as utils]
            [sixsq.nuvla.ui.job.events :as job-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))



; Perform form validation if validate-form? is true.


(reg-event-db
  ::validate-credential-form
  (fn [db [_ form-spec]]
    (let [credential     (get db ::spec/credential)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form? (if (nil? form-spec) true (s/valid? form-spec credential)) true)]
      (s/explain form-spec credential)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-db
  ::set-validate-form?
  (fn [db [_ validate-form?]]
    (assoc db ::spec/validate-form? validate-form?)))


; Set the spec to apply for form validation

(reg-event-db
  ::set-form-spec
  (fn [db [_ form-spec]]
    (assoc db ::spec/form-spec form-spec)))


(reg-event-db
  ::active-input
  (fn [db [_ input-name]]
    (assoc db ::spec/active-input input-name)))


(reg-event-db
  ::form-valid
  (fn [db [_]]
    (assoc db ::spec/form-valid? true)))


(reg-event-db
  ::set-credential
  (fn [db [_ credential]]
    (assoc db ::spec/credential credential)))


(reg-event-db
  ::set-credentials
  (fn [db [_ credentials]]
    (assoc db ::spec/credentials credentials
              ::main-spec/loading? false)))


(reg-event-fx
  ::get-credentials
  (fn [_ _]
    {::cimi-api-fx/search [:credential
                           {:orderby "name:asc, id:asc"}
                           #(dispatch [::set-credentials (:resources %)])]}))


(reg-event-db
  ::set-credentials-summary
  (fn [db [_ credentials]]
    (assoc db ::spec/credentials-summary credentials)))


"FIXME: aggregation is limited to 10 terms. In this case, there are more than 10 terms for
  credentials. Filter first and aggregate second, and do this twice?"
(reg-event-fx
  ::get-credentials-summary
  (fn [_ _]
    {::cimi-api-fx/search [:credential
                           {:orderby     "name:asc, id:asc"
                            :aggregation "terms:subtype"
                            :first       0
                            :last        0}
                           #(dispatch [::set-credentials-summary %])]}))


(reg-event-db
  ::set-generated-credential-modal
  (fn [db [_ credential]]
    (assoc db ::spec/generated-credential-modal credential)))


(reg-event-fx
  ::edit-credential
  (fn [{{:keys [::spec/credential] :as db} :db}]
    (let [id (:id credential)]
      (if (nil? id)
        (let [new-credential (utils/db->new-credential db)]
          {::cimi-api-fx/add [:credential new-credential
                              #(do (dispatch [::cimi-detail-events/get (:resource-id %)])
                                   (dispatch [::close-credential-modal])
                                   (dispatch [::get-credentials])
                                   (if (utils/show-generated-cred-modal? new-credential)
                                     (dispatch [::set-generated-credential-modal %])
                                     (let [{:keys [status message resource-id]} (response/parse %)]
                                       (dispatch [::messages-events/add
                                                  {:header  (cond-> (str "added " resource-id)
                                                                    status (str " (" status ")"))
                                                   :content message
                                                   :type    :success}])))
                                   )]})
        {::cimi-api-fx/edit [id credential
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::cimi-detail-events/get (:id %)])
                                    (dispatch [::close-credential-modal])
                                    (dispatch [::get-credentials])))]}))))


(reg-event-fx
  ::delete-credential
  (fn [{:keys [db]} [_ id]]
    {:db                  db
     ::cimi-api-fx/delete [id #(dispatch [::get-credentials])]}))


(reg-event-db
  ::open-add-credential-modal
  (fn [db _]
    (assoc db ::spec/add-credential-modal-visible? true)))


(reg-event-db
  ::close-add-credential-modal
  (fn [db _]
    (assoc db ::spec/add-credential-modal-visible? false)))


(reg-event-db
  ::open-credential-modal
  (fn [db [_ credential is-new?]]
    (-> db
        (assoc ::spec/credential credential)
        (assoc ::spec/credential-modal-visible? true)
        (assoc ::spec/is-new? is-new?))))


(reg-event-db
  ::close-credential-modal
  (fn [db _]
    (assoc db ::spec/credential-modal-visible? false)))


(reg-event-db
  ::update-credential
  (fn [db [_ key value]]
    (assoc-in db [::spec/credential key] value)))


(reg-event-fx
  ::set-infrastructure-services-available
  (fn [{db :db} [_ response]]
    (let [infrastructure-services (:resources response)]
      (cond-> {:db (assoc db ::spec/infrastructure-services-available infrastructure-services)}
              (= (:count response) 1) (assoc :dispatch
                                             [::update-credential :parent
                                              (-> infrastructure-services first :id)])))))


(reg-event-fx
  ::fetch-infrastructure-services-available
  (fn [{:keys [db]} [_ subtypes additional-filter]]
    {:db                  (assoc db ::spec/infrastructure-services-available nil)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter (cond-> (apply general-utils/join-or
                                                   (map #(str "subtype='" % "'") subtypes))
                                            additional-filter (general-utils/join-and
                                                                additional-filter))
                            :last   10000}
                           #(dispatch [::set-infrastructure-services-available %])]}))


(reg-event-db
  ::set-credential-after-check
  (fn [{:keys [::spec/credential-check-table] :as db}
       [_ {:keys [id last-check status] :as _credential}]]
    (assoc-in db [::spec/credential-check-table id]
              (assoc (get credential-check-table id)
                :last-check last-check
                :status status
                :check-in-progress? false))))


(reg-event-fx
  ::check-credential-complete
  (fn [{db :db} [_ {:keys [target-resource status-message return-code] :as _job}]]
    (let [id (:href target-resource)]
      (cond->
        {::cimi-api-fx/get [id #(dispatch [::set-credential-after-check %])]}
        (not= return-code 0) (assoc :db (assoc-in db [::spec/credential-check-table id :error-msg]
                                                  status-message))))))


(reg-event-fx
  ::check-credential
  (fn [{{:keys [::spec/credential-check-table] :as db} :db}
       [_ {:keys [id] :as credential} delta-minutes-outdated]]
    (let [{:keys [last-check status]} (get credential-check-table id)
          cred        (cond-> credential
                              last-check (assoc :last-check last-check)
                              status (assoc :status status))
          need-check? (utils/credential-need-check? cred delta-minutes-outdated)
          on-success  (fn [response]
                        (dispatch
                          [::job-events/wait-job-to-complete
                           {:job-id      (:location response)
                            :on-complete #(dispatch [::check-credential-complete %])}]))]
      (cond-> {:db (assoc-in db [::spec/credential-check-table id]
                             {:check-in-progress? need-check?
                              :error-msg          nil
                              :status             (:status cred)
                              :last-check         (:last-check cred)})}
              need-check? (assoc ::cimi-api-fx/operation [id "check" on-success])))))
