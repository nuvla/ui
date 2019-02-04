(ns sixsq.slipstream.webui.authn.events
  (:require
    [ajax.core :as ajax]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.authn.effects :as authn-fx]
    [sixsq.slipstream.webui.authn.spec :as authn-spec]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.history.effects :as history-fx]
    [taoensso.timbre :as log]))


(reg-event-fx
  ::initialize
  (fn [{:keys [db]} _]
    (when-let [client (::client-spec/client db)]
      {::cimi-api-fx/session [client #(dispatch [::set-session %])]})))


(reg-event-fx
  ::set-session
  (fn [{:keys [db]} [_ {:keys [username] :as session}]]
    (let [redirect-uri (::authn-spec/redirect-uri db)
          client (::client-spec/client db)]
      (cond-> {:db (assoc db ::authn-spec/session session)}

              (and session redirect-uri)
              (assoc ::history-fx/navigate-js-location [redirect-uri])

              session
              (assoc ::cimi-api-fx/current-user-params
                     [client username #(dispatch [::set-current-user-params %])]
                     ::authn-fx/automatic-logout-at-session-expiry [session])))))


(reg-event-fx
  ::reset-password
  (fn [db [_ username]]
    (dispatch [::set-loading])
    {:http-xhrio {:method          :post
                  :uri             "https://nuv.la/reset" ; FIXME: should use a configuration parameter for http://nuv.la, or move reset to /api
                  :params          {:username @username}
                  :format          (ajax/url-request-format)
                  :response-format (ajax/text-response-format {:keywords? true})
                  :on-success      [::set-success-message]
                  :on-failure      [::set-error-message]}}))


(reg-event-db
  ::set-current-user-params
  (fn [db [_ user]]
    (assoc db ::authn-spec/current-user-params user)))


(reg-event-fx
  ::logout
  (fn [cofx _]
    (when-let [client (-> cofx :db ::client-spec/client)]
      {::cimi-api-fx/logout [client (fn []
                                      (dispatch [::set-session nil])
                                      (dispatch [::set-current-user-params nil]))]})))


(reg-event-db
  ::open-modal
  (fn [db [_ modal-key]]
    (assoc db ::authn-spec/open-modal modal-key)))


(reg-event-db
  ::close-modal
  (fn [db _]
    (assoc db ::authn-spec/open-modal nil
              ::authn-spec/selected-method-group nil)))


(reg-event-db
  ::set-selected-method-group
  (fn [db [_ selected-method]]
    (assoc db ::authn-spec/selected-method-group selected-method)))


(reg-event-db
  ::clear-loading
  (fn [db _]
    (assoc db ::authn-spec/loading? false)))


(reg-event-db
  ::set-loading
  (fn [db _]
    (assoc db ::authn-spec/loading? true)))


(reg-event-db
  ::set-error-message
  (fn [db [_ error-message]]
    (assoc db ::authn-spec/error-message error-message)))


(reg-event-db
  ::clear-error-message
  (fn [db _]
    (assoc db ::authn-spec/error-message nil)))


(reg-event-db
  ::set-success-message
  (fn [db [_ success-message]]
    (dispatch [::clear-loading])
    (assoc db ::authn-spec/success-message success-message)))


(reg-event-db
  ::clear-success-message
  (fn [db _]
    (dispatch [::clear-loading])
    (assoc db ::authn-spec/success-message nil)))


(reg-event-db
  ::redirect-uri
  (fn [db [_ uri]]
    (assoc db ::authn-spec/redirect-uri uri)))


(reg-event-db
  ::server-redirect-uri
  (fn [db [_ uri]]
    (assoc db ::authn-spec/server-redirect-uri uri)))


(reg-event-db
  ::set-form-id
  (fn [db [_ form-id]]
    (assoc db ::authn-spec/form-id form-id)))
