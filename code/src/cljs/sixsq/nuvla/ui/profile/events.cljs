(ns sixsq.nuvla.ui.profile.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.i18n.spec :as i18n-spec]
    [sixsq.nuvla.ui.profile.effects :as fx]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.profile.spec :as spec]
    [sixsq.nuvla.ui.session.spec :as session-spec]
    [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-fx
  ::init
  (fn [{db :db} _]
    {:db              (merge db spec/defaults)
     ::fx/load-stripe ["pk_test_Wo4so0qa2wqn66052FlvyMpl00MhPPQdAG"
                       #(dispatch [::load-stripe-done %])]
     :dispatch-n      [[::get-user]
                       [::search-existing-customer]]}))


(reg-event-db
  ::load-stripe-done
  (fn [db [_ stripe]]
    (assoc db ::spec/stripe stripe)))


(reg-event-db
  ::set-user
  (fn [{:keys [::spec/loading] :as db} [_ user]]
    (assoc db ::spec/user user
              ::spec/loading (disj loading :user))))


(reg-event-fx
  ::get-user
  (fn [{{:keys [::session-spec/session] :as db} :db} _]
    (when-let [user (:user session)]
      {:db               (update db ::spec/loading conj :user)
       ::cimi-api-fx/get [user #(dispatch [::set-user %])]})))


(reg-event-fx
  ::get-customer
  (fn [{db :db} [_ id]]
    {:db               (-> db
                           (update ::spec/loading disj :create-customer)
                           (update ::spec/loading conj :customer))
     ::cimi-api-fx/get [id #(dispatch [::set-customer %])]}))


(reg-event-fx
  ::set-customer
  (fn [{{:keys [::spec/loading] :as db} :db} [_ customer]]
    (cond-> {:db (assoc db ::spec/customer customer
                           ::spec/loading (disj loading :customer))}
            customer (assoc :dispatch-n [[::get-subscription]
                                         [::list-payment-methods]
                                         [::close-modal]]))))


(reg-event-fx
  ::search-existing-customer
  (fn [_ _]
    {::cimi-api-fx/search [:customer {} #(if-let [id (-> % :resources first :id)]
                                           (dispatch [::get-customer id])
                                           (dispatch [::set-customer nil]))]}))


(reg-event-fx
  ::get-subscription
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    (js/console.log ::get-subscription)
    {:db                     (update db ::spec/loading conj :subscription)
     ::cimi-api-fx/operation [(:id customer) "get-subscription"
                              #(dispatch [::set-subscription %])]}))


(reg-event-db
  ::set-subscription
  (fn [{:keys [::spec/loading] :as db} [_ subscription]]
    (js/console.log ::set-subscription)
    (assoc db ::spec/subscription subscription
              ::spec/loading (disj loading :subscription))))


(reg-event-fx
  ::list-payment-methods
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    {:db                     (update db ::spec/loading conj :payment-methods)
     ::cimi-api-fx/operation [(:id customer) "list-payment-methods"
                              #(dispatch [::set-payment-methods %])]}))


(reg-event-db
  ::set-payment-methods
  (fn [{:keys [::spec/loading] :as db} [_ payment-methods]]
    (assoc db ::spec/payment-methods payment-methods
              ::spec/loading (disj loading :payment-methods))))


(reg-event-db
  ::open-modal
  (fn [db [_ modal-key]]
    (assoc db ::spec/open-modal modal-key)))


(reg-event-db
  ::close-modal
  (fn [db _]
    (assoc db ::spec/open-modal nil
              ::spec/error-message nil
              ::spec/setup-intent nil)))


(reg-event-db
  ::set-error-message
  (fn [db [_ error-message]]
    (assoc db ::spec/error-message error-message)))


(reg-event-db
  ::clear-error-message
  (fn [db _]
    (assoc db ::spec/error-message nil)))


(reg-event-fx
  ::change-password
  (fn [{{:keys [::spec/user
                ::i18n-spec/tr]} :db} [_ body]]
    (let [callback-fn #(if (instance? js/Error %)
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
      {::cimi-api-fx/operation [(:credential-password user) "change-password" callback-fn body]})))


(reg-event-fx
  ::create-payment-method
  (fn [{{:keys [::spec/stripe] :as db} :db} [_ data]]
    (when stripe
      {::fx/create-payment-method [stripe data #(dispatch [::set-payment-method-result %])]
       :db                        (update db ::spec/loading conj :create-payment)})))


(reg-event-fx
  ::set-payment-method-result
  (fn [{{:keys [::spec/loading] :as db} :db} [_ result]]
    (js/console.log ::set-payment-method-result result)
    (let [res            (-> result (js->clj :keywordize-keys true))
          error          (:error res)
          payment-method (:paymentMethod res)]
      (if error
        {:db (assoc db ::spec/error-message (:message error)
                       ::spec/loading (disj loading :create-payment))}
        {:dispatch [::create-customer (:id payment-method)]}))))


(reg-event-fx
  ::create-customer
  (fn [{db :db} [_ payment-id]]
    {:db               (update db ::spec/loading conj :create-customer)
     ::cimi-api-fx/add [:customer {:plan-id           "plan_HGQ9iUgnz2ho8e"
                                   :plan-item-ids     ["plan_HGQIIWmhYmi45G"
                                                       "plan_HIrgmGboUlLqG9"
                                                       "plan_HGQAXewpgs9NeW"
                                                       "plan_HGQqB0p8h86Ija"]
                                   :payment-method-id payment-id}
                        #(dispatch [::get-customer (:resource-id %)])]}))


(reg-event-fx
  ::confirm-card-setup
  (fn [{{:keys [::spec/stripe
                ::spec/setup-intent] :as db} :db} [_ data]]
    (when stripe
      {::fx/confirm-card-setup [stripe (:client-secret setup-intent)
                                data #(dispatch [::set-confirm-card-setup-result %])]
       :db                     (update db ::spec/loading conj :confirm-card-setup)})))


(reg-event-fx
  ::set-confirm-card-setup-result
  (fn [{{:keys [::spec/loading
                ::spec/customer] :as db} :db} [_ result]]
    (let [res   (-> result (js->clj :keywordize-keys true))
          error (:error res)]
      (if error
        {:db (assoc db ::spec/error-message (:message error)
                       ::spec/loading (disj loading :confirm-card-setup))}
        {:dispatch-n [[::list-payment-methods]
                      [::close-modal]]}))))


(reg-event-fx
  ::confirm-iban-setup
  (fn [{{:keys [::spec/stripe
                ::spec/setup-intent] :as db} :db} [_ data]]
    (when stripe
      {::fx/confirm-sepa-debit-setup [stripe (:client-secret setup-intent)
                                      data #(dispatch [::set-confirm-iban-setup-result %])]
       :db                           (update db ::spec/loading conj :confirm-card-setup)})))


(reg-event-fx
  ::set-confirm-iban-setup-result
  (fn [{{:keys [::spec/loading
                ::spec/customer] :as db} :db} [_ result]]
    (let [res   (-> result (js->clj :keywordize-keys true))
          error (:error res)]
      (if error
        {:db (assoc db ::spec/error-message (:message error)
                       ::spec/loading (disj loading :confirm-card-setup))}
        {:dispatch-n [[::list-payment-methods]
                      [::close-modal]]}))))



(reg-event-db
  ::set-setup-intent
  (fn [{:keys [::spec/loading] :as db} [_ setup-itent]]
    (assoc db ::spec/setup-intent setup-itent
              ::spec/loading (disj loading :create-setup-intent))))


(reg-event-fx
  ::create-setup-intent
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    {:db                     (update db ::spec/loading conj :create-setup-intent)
     ::cimi-api-fx/operation [(:id customer) "create-setup-intent"
                              #(dispatch [::set-setup-intent %])]}))


(reg-event-fx
  ::detach-payment-method
  (fn [{{:keys [::spec/customer] :as db} :db} [_ payment-method]]
    {::cimi-api-fx/operation [(:id customer) "detach-payment-method"
                              #(dispatch [::list-payment-methods])
                              {:payment-method payment-method}]}))


(reg-event-fx
  ::set-default-payment-method
  (fn [{{:keys [::spec/customer] :as db} :db} [_ payment-method]]
    {::cimi-api-fx/operation [(:id customer) "set-default-payment-method"
                              #(dispatch [::list-payment-methods])
                              {:payment-method payment-method}]}))


