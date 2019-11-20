(ns sixsq.nuvla.ui.credentials.events
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
    [sixsq.nuvla.ui.credentials.spec :as spec]
    [sixsq.nuvla.ui.credentials.utils :as utils]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))



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
    (assoc db ::spec/credentials credentials)))


(reg-event-fx
  ::get-credentials
  (fn [{:keys [db]} [_]]
    {:db                  (assoc db ::spec/completed? false)
     ::cimi-api-fx/search [:credential {} #(dispatch [::set-credentials (:resources %)])]}))


(reg-event-db
  ::set-generated-credential-modal
  (fn [db [_ credential]]
    (assoc db ::spec/generated-credential-modal credential)))


(reg-event-fx
  ::edit-credential
  (fn [{{:keys [::spec/credential] :as db} :db} [_]]
    (let [id             (:id credential)
          new-credential (utils/db->new-credential db)]
      (if (nil? id)
        {:db               db
         ::cimi-api-fx/add [:credential new-credential
                            #(do (dispatch [::cimi-detail-events/get (:resource-id %)])
                                 (dispatch [::close-credential-modal])
                                 (dispatch [::get-credentials])
                                 (dispatch [::main-events/check-bootstrap-message])
                                 (when
                                   (contains?
                                     #{"credential-template/create-credential-vpn-customer"}
                                     (get-in new-credential [:template :href]))
                                   (dispatch [::set-generated-credential-modal %]))
                                 )]}
        {:db                db
         ::cimi-api-fx/edit [id credential
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
  ::open-delete-confirmation-modal
  (fn [db [_ id credential]]
    (-> db
        (assoc ::spec/credential credential)
        (assoc ::spec/delete-confirmation-modal-visible? true))))


(reg-event-db
  ::close-delete-confirmation-modal
  (fn [db _]
    (assoc db ::spec/delete-confirmation-modal-visible? false)))


(reg-event-db
  ::update-credential
  (fn [db [_ key value]]
    (assoc-in db [::spec/credential key] value)))


(reg-event-db
  ::set-infrastructure-services-available
  (fn [db [_ subtype response]]
    (let [infrastructure-services (:resources response)]
      (assoc-in db [::spec/infrastructure-services-available subtype] infrastructure-services))))


(reg-event-fx
  ::fetch-infrastructure-services-available
  (fn [{:keys [db]} [_ subtype additional-filter]]
    {:db                  (assoc-in db [::spec/infrastructure-services-available subtype] nil)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter (cond-> (str "subtype='" subtype "'")
                                            additional-filter (general-utils/join-and
                                                                additional-filter))
                            :last   10000}
                           #(dispatch [::set-infrastructure-services-available subtype %])]}))
