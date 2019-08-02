(ns sixsq.nuvla.ui.authn.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.authn.effects :as fx]
    [sixsq.nuvla.ui.authn.spec :as spec]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi.events :as cimi-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


(reg-event-fx
  ::initialize
  (fn [_ _]
    {::cimi-api-fx/session
     [(fn [session]
        (dispatch [::set-session session])
        (when session
          (dispatch [:sixsq.nuvla.ui.main.events/check-bootstrap-message])))]}))


(reg-event-fx
  ::set-session
  (fn [{{:keys [::spec/redirect-uri
                ::spec/session
                ::main-spec/nav-path
                ::main-spec/pages] :as db} :db} [_ session-arg]]
    (let [no-session-protected-page? (and (nil? session-arg)
                                          (->> nav-path first (get pages) :protected?))]
      (cond-> {:db (assoc db ::spec/session session-arg)}
              no-session-protected-page? (assoc :dispatch [::open-modal :login])
              session-arg (assoc ::fx/automatic-logout-at-session-expiry [session-arg])
              ;; force refresh templates collection cache when not the same user (different session)
              (not= session session-arg) (assoc :dispatch-n [[::cimi-events/get-cloud-entry-point]
                                                             [:sixsq.nuvla.ui.main.events/force-refresh-content]])))))


(reg-event-fx
  ::logout
  (fn [{:keys [db]} _]
    {:db                  (assoc db :sixsq.nuvla.ui.main.spec/bootstrap-message nil)
     ::cimi-api-fx/logout [#(dispatch [::set-session nil])]}))


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
  ::close-modal-no-session
  (fn [{:keys [::spec/open-modal] :as db} _]
    (when-not (contains? #{:invite-user :reset-password} open-modal)
      (dispatch [::close-modal]))
    db))


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


(reg-event-fx
  ::set-success-message
  (fn [{db :db} [_ success-message]]
    {:db       (assoc db ::spec/success-message success-message)
     :dispatch [::clear-loading]}))


(reg-event-db
  ::set-form-id
  (fn [db [_ form-id]]
    (assoc db ::spec/form-id form-id
              ::spec/form-data nil)))


(reg-event-db
  ::update-form-data
  (fn [db [_ param-name param-value]]
    (update db ::spec/form-data assoc param-name param-value)))


(reg-event-db
  ::clear-form-data
  (fn [db _]
    (assoc db ::spec/form-data {})))


(defn default-submit-callback
  [close-modal success-msg response]
  (dispatch [::clear-loading])
  (dispatch [::clear-form-data])
  (dispatch [::initialize])
  (when close-modal
    (dispatch [::close-modal]))
  (when success-msg
    (dispatch [::set-success-message success-msg])))


(reg-event-fx
  ::submit
  (fn [{{:keys [::spec/form-id
                ::spec/form-data
                ::spec/server-redirect-uri] :as db} :db} [_ opts]]
    (let [{close-modal  :close-modal,
           success-msg  :success-msg,
           callback-add :callback-add,
           redirect-url :redirect-url
           :or          {close-modal  true
                         redirect-url server-redirect-uri}} opts

          on-success    (or callback-add
                            (partial default-submit-callback close-modal success-msg))

          on-error      #(let [{:keys [message]} (response/parse-ex-info %)]
                           (dispatch [::clear-loading])
                           (dispatch [::set-error-message (or message %)]))

          template      {:template (-> form-data
                                       (dissoc :repeat-new-password
                                               :repeat-password)
                                       (assoc :href form-id
                                              :redirect-url redirect-url))}
          collection-kw (cond
                          (str/starts-with? form-id "session-template/") :session
                          (str/starts-with? form-id "user-template/") :user)]

      {:db               (assoc db ::spec/loading? true
                                   ::spec/success-message nil
                                   ::spec/error-message nil)
       ::cimi-api-fx/add [collection-kw template on-success :on-error on-error]})))
