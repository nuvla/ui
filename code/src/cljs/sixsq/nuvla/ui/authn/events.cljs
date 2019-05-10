(ns sixsq.nuvla.ui.authn.events
  (:require
    [clojure.string :as str]
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
  (fn [{{:keys [::client-spec/client] :as db} :db} _]
    (when client
      {::cimi-api-fx/session [client #(do (dispatch [::set-session %])
                                          (dispatch [:sixsq.nuvla.ui.main.events/check-bootstrap-message]))]})))


(reg-event-fx
  ::set-session
  (fn [{:keys [db]} [_ session]]
    (let [redirect-uri (::spec/redirect-uri db)]
      (cond-> {:db (assoc db ::spec/session session)}
              session (assoc ::fx/automatic-logout-at-session-expiry [session])
              (and session redirect-uri) (assoc ::history-fx/navigate-js-location [redirect-uri])))))


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
              ::spec/form-data nil
              ::spec/error-message nil
              ::spec/success-message nil)))


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
    (assoc db ::spec/form-id form-id
              ::spec/form-data nil)))


(reg-event-db
  ::update-form-data
  (fn [db [_ param-name param-value]]
    (update db ::spec/form-data assoc param-name param-value)))


(reg-event-fx
  ::submit
  (fn [{{:keys [::client-spec/client
                ::spec/form-id
                ::spec/form-data
                ::spec/server-redirect-uri] :as db} :db} [_ opts]]
    (let [{close-modal :close-modal,
           success-msg :success-msg,
           :or         {close-modal true}} opts
          {callback-add :callback-add,
           :or          {callback-add #(if (instance? js/Error %)
                                         (let [{:keys [message]} (response/parse-ex-info %)]
                                           (dispatch [::set-error-message message]))
                                         (do (dispatch [::initialize])
                                             (when close-modal
                                               (dispatch [::close-modal]))
                                             (when success-msg
                                               (dispatch [::set-success-message success-msg]))
                                             (dispatch [::history-events/navigate "welcome"])))}} opts
          template      {:template (-> form-data
                                       (dissoc :repeat-new-password
                                               :repeat-password)
                                       (assoc :href form-id
                                              :redirect-url server-redirect-uri))}
          collection-kw (cond
                          (str/starts-with? form-id "session-template/") :session
                          (str/starts-with? form-id "user-template/") :user)]
      {::cimi-api-fx/add [client collection-kw template callback-add]})))