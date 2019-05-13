(ns sixsq.nuvla.ui.profile.events
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.authn.spec :as authn-spec]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.i18n.spec :as i18n-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.profile.spec :as spec]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


(reg-event-db
  ::open-modal
  (fn [db [_ modal-key]]
    (assoc db ::spec/open-modal modal-key)))


(reg-event-db
  ::close-modal
  (fn [db _]
    (assoc db ::spec/open-modal nil
              ::spec/form-data nil
              ::spec/error-message nil)))


(reg-event-db
  ::set-error-message
  (fn [db [_ error-message]]
    (assoc db ::spec/error-message error-message)))


(reg-event-db
  ::clear-error-message
  (fn [db _]
    (assoc db ::spec/error-message nil)))


(reg-event-db
  ::update-form-data
  (fn [db [_ param-name param-value]]
    (update db ::spec/form-data assoc param-name param-value)))


(reg-event-db
  ::set-password
  (fn [db [_ user]]
    (assoc db ::spec/credential-password (:credential-password user))))


(reg-event-fx
  ::get-user
  (fn [{{:keys [::client-spec/client
                ::authn-spec/session] :as db} :db} _]
    (when-let [user (:user session)]
      {::cimi-api-fx/get [client user #(dispatch [::set-password %])]})))


(reg-event-fx
  ::change-password
  (fn [{{:keys [::client-spec/client
                ::spec/form-data
                ::spec/credential-password
                ::i18n-spec/tr] :as db} :db} _]
    (let [body        (dissoc form-data :repeat-new-password)
          callback-fn #(if (instance? js/Error %)
                         (let [{:keys [message]} (response/parse-ex-info %)]
                           (dispatch [::set-error-message message]))
                         (let [{:keys [status message]} %]
                           (if (= status 200)
                             (do
                               (dispatch [::close-modal])
                               (dispatch [::messages-events/add
                                          {:header  (str/capitalize (tr [:success]))
                                           :content (str/capitalize (tr [:password-updated]))
                                           :type    :success}]))
                             (dispatch [::set-error-message (str message " (" status ")")]))))]
      {::cimi-api-fx/operation [client credential-password "change-password" callback-fn body]})))
