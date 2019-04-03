(ns sixsq.nuvla.ui.authn.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.authn.effects :as fx]
    [sixsq.nuvla.ui.authn.spec :as spec]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.history.effects :as history-fx]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-fx
  ::initialize
  (fn [{:keys [db]} _]
    (when-let [client (::client-spec/client db)]
      {::cimi-api-fx/session [client #(dispatch [::set-session %])]})))


(reg-event-fx
  ::set-session
  (fn [{:keys [db]} [_ session]]
    (let [redirect-uri (::spec/redirect-uri db)]
      (cond-> {:db (assoc db ::spec/session session)}

              (and session redirect-uri) (assoc ::history-fx/navigate-js-location [redirect-uri])

              session (assoc ::fx/automatic-logout-at-session-expiry [session])))))


(reg-event-fx
  ::reset-password
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ username new-password]]
    (let [callback-add #(do (if (instance? js/Error %)
                              (let [{:keys [message]} (response/parse-ex-info %)]
                                (dispatch [::set-error-message message]))
                              (dispatch [::set-success-message
                                         "Success! An reset message has been sent to your email account."]))
                            (dispatch [::clear-loading]))
          template {:template {:href         "session-template/password-reset"
                               :username     username
                               :new-password new-password}}]
      {:db               (assoc db ::spec/loading? true)
       ::cimi-api-fx/add [client :session template callback-add]})))


(reg-event-fx
  ::logout
  (fn [cofx _]
    (when-let [client (-> cofx :db ::client-spec/client)]
      {::cimi-api-fx/logout [client (fn []
                                      (dispatch [::set-session nil]))]})))


(reg-event-db
  ::open-modal
  (fn [db [_ modal-key]]
    (assoc db ::spec/open-modal modal-key)))


(reg-event-db
  ::close-modal
  (fn [db _]
    (assoc db ::spec/open-modal nil
              ::spec/selected-method-group nil
              ::spec/form-data nil)))


(reg-event-db
  ::set-selected-method-group
  (fn [db [_ selected-method]]
    (assoc db ::spec/selected-method-group selected-method)))


(reg-event-db
  ::clear-loading
  (fn [db _]
    (assoc db ::spec/loading? false)))

(reg-event-db
  ::set-error-message
  (fn [db [_ error-message]]
    (assoc db ::spec/error-message error-message)))


(reg-event-db
  ::clear-error-message
  (fn [db _]
    (assoc db ::spec/error-message nil)))


(reg-event-db
  ::set-success-message
  (fn [db [_ success-message]]
    (dispatch [::clear-loading])
    (assoc db ::spec/success-message success-message)))


(reg-event-db
  ::clear-success-message
  (fn [db _]
    (dispatch [::clear-loading])
    (assoc db ::spec/success-message nil)))


(reg-event-db
  ::redirect-uri
  (fn [db [_ uri]]
    (assoc db ::spec/redirect-uri uri)))


(reg-event-db
  ::server-redirect-uri
  (fn [db [_ uri]]
    (assoc db ::spec/server-redirect-uri uri)))


(reg-event-db
  ::set-form-id
  (fn [db [_ form-id]]
    (assoc db ::spec/form-id form-id)))


(reg-event-db
  ::update-form-data
  (fn [db [_ form-id param-name param-value]]
    (update db ::spec/form-data assoc-in [form-id param-name] param-value)))


(reg-event-fx
  ::submit
  (fn [{{:keys [::client-spec/client
                ::spec/form-data] :as db} :db} [_ collection-kw form-id]]
    (let [template {:template (-> form-data
                                  (get form-id)
                                  (assoc :href form-id))}
          callback-add #(if (instance? js/Error %)
                          (let [{:keys [message]} (response/parse-ex-info %)]
                            (dispatch [::set-error-message message]))
                          (do (dispatch [::initialize])
                              (dispatch [::close-modal])
                              (dispatch [::history-events/navigate "welcome"])
                              (dispatch [::clear-success-message])
                              (dispatch [::clear-error-message])))]
      {::cimi-api-fx/add [client collection-kw template callback-add]})))