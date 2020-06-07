(ns sixsq.nuvla.ui.profile.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.i18n.spec :as i18n-spec]
    [sixsq.nuvla.ui.profile.effects :as fx]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    ["@stripe/react-stripe-js" :as react-stripe]
    [sixsq.nuvla.ui.profile.spec :as spec]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.session.spec :as session-spec]
    [sixsq.nuvla.ui.utils.response :as response]))

;; TODO when customer exist but not valid subscription

(reg-event-fx
  ::init
  (fn [{db :db} _]
    {:db         (merge db spec/defaults)
     :dispatch-n [[::get-user]
                  [::search-existing-customer]]}))


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
    {:db                     (update db ::spec/loading conj :subscription)
     ::cimi-api-fx/operation [(:id customer) "get-subscription"
                              #(dispatch [::set-subscription %])]}))


(reg-event-db
  ::set-subscription
  (fn [{:keys [::spec/loading] :as db} [_ subscription]]
    (assoc db ::spec/subscription subscription
              ::spec/loading (disj loading :subscription))))


(reg-event-fx
  ::customer-info
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    {:db                     (update db ::spec/loading conj :customer-info)
     ::cimi-api-fx/operation [(:id customer) "customer-info"
                              #(dispatch [::set-customer-info %])]}))


(reg-event-db
  ::set-customer-info
  (fn [{:keys [::spec/loading] :as db} [_ customer-info]]
    (assoc db ::spec/customer-info customer-info
              ::spec/loading (disj loading :customer-info))))


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


(reg-event-fx
  ::upcoming-invoice
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    {:db                     (update db ::spec/loading conj :upcoming-invoice)
     ::cimi-api-fx/operation [(:id customer) "upcoming-invoice"
                              #(dispatch [::set-upcoming-invoice %])]}))


(reg-event-db
  ::set-upcoming-invoice
  (fn [{:keys [::spec/loading] :as db} [_ upcoming-invoice]]
    (assoc db ::spec/upcoming-invoice upcoming-invoice
              ::spec/loading (disj loading :upcoming-invoice))))


(reg-event-fx
  ::list-invoices
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    {:db                     (update db ::spec/loading conj :invoices)
     ::cimi-api-fx/operation [(:id customer) "list-invoices"
                              #(dispatch [::set-invoices %])]}))


(reg-event-db
  ::set-invoices
  (fn [{:keys [::spec/loading] :as db} [_ invoices]]
    (assoc db ::spec/invoices invoices
              ::spec/loading (disj loading :invoices))))


(reg-event-db
  ::open-modal
  (fn [db [_ modal-key]]
    (assoc db ::spec/open-modal modal-key)))


(reg-event-db
  ::close-modal
  (fn [db _]
    (assoc db ::spec/open-modal nil
              ::spec/error-message nil)))


(reg-event-db
  ::clear-error-message
  (fn [db _]
    (assoc db ::spec/error-message nil)))


(reg-event-fx
  ::set-error
  (fn [{db :db} [_ error-msg key-loading]]
    {:db (cond-> (assoc db ::spec/error-message error-msg)
                 key-loading (update ::spec/loading disj key-loading))}))


(reg-event-fx
  ::change-password
  (fn [{{:keys [::spec/user
                ::i18n-spec/tr]} :db} [_ body]]
    (let [callback-fn #(if (instance? js/Error %)
                         (dispatch [::set-error (-> % response/parse-ex-info :message)])
                         (let [{:keys [status message]} %]
                           (if (= status 200)
                             (do
                               (dispatch [::close-modal])
                               (dispatch [::messages-events/add
                                          {:header  (str/capitalize (tr [:success]))
                                           :content (str/capitalize (tr [:password-updated]))
                                           :type    :success}]))
                             (dispatch [::set-error (str message " (" status ")")]))))]
      {::cimi-api-fx/operation [(:credential-password user) "change-password" callback-fn body]})))


(reg-event-fx
  ::create-payment-method
  (fn [{{:keys [::main-spec/stripe] :as db} :db} [_ event-kw {:keys [payment-method] :as resource}]]
    (if payment-method
      (let [{input-type :type elements :elements} payment-method
            data (clj->js
                   {:type            input-type
                    input-type       (case input-type
                                       "sepa_debit" (.getElement elements react-stripe/IbanElement)
                                       "card" (.getElement elements react-stripe/CardElement))
                    :billing_details (when (= input-type "sepa_debit")
                                       {:name  "test"
                                        :email "test@example.com"})})]
        {::fx/create-payment-method [stripe data
                                     #(dispatch [::set-payment-method-result event-kw resource %])]
         :db                        (update db ::spec/loading conj :create-payment)})
      {:dispatch [event-kw resource]})))


(reg-event-fx
  ::set-payment-method-result
  (fn [{db :db} [_ event-kw resource result]]
    (let [res            (-> result (js->clj :keywordize-keys true))
          error          (:error res)
          payment-method (-> res :paymentMethod :id)]
      (if error
        {:dispatch [::set-error (:message error) :create-payment]}
        {:db       (update db ::spec/loading disj :create-payment)
         :dispatch [event-kw (assoc resource :payment-method payment-method)]}))))


(defn catalogue->subscription
  [pricing-catalogue]
  (let [pay-as-you-go (-> pricing-catalogue :plans first)]
    {:plan-id       (:plan-id pay-as-you-go)
     :plan-item-ids (:required-items pay-as-you-go)}))


(reg-event-fx
  ::create-customer
  (fn [{{:keys [::spec/pricing-catalogue] :as db} :db} [_ {:keys [payment-method] :as customer}]]
    {:db               (update db ::spec/loading conj :create-customer)
     ::cimi-api-fx/add [:customer (cond-> (assoc customer :subscription (catalogue->subscription
                                                                          pricing-catalogue))
                                          payment-method (assoc :payment-method payment-method))
                        #(dispatch [::get-customer (:resource-id %)])
                        :on-error #(dispatch [::set-error (-> % response/parse-ex-info :message)
                                              :create-customer])]}))


(reg-event-fx
  ::create-subscription
  (fn [{{:keys [::spec/pricing-catalogue
                ::spec/customer] :as db} :db} _]
    {:db                     (update db ::spec/loading conj :create-customer)
     ::cimi-api-fx/operation [(:id customer)
                              "create-subscription"
                              #(if (instance? js/Error %)
                                 (dispatch [::set-error (-> % response/parse-ex-info :message)
                                            :create-customer])
                                 (dispatch [::get-customer (:id customer)]))
                              (catalogue->subscription pricing-catalogue)]}))


(reg-event-fx
  ::get-pricing-catalogue
  (fn [_ _]
    {::cimi-api-fx/get ["pricing/catalogue" #(dispatch [::set-pricing-catalogue %])]}))


(reg-event-db
  ::set-pricing-catalogue
  (fn [db [_ resource]]
    (assoc db ::spec/pricing-catalogue resource)))


(reg-event-fx
  ::confirm-card-setup
  (fn [{{:keys [::main-spec/stripe
                ::spec/setup-intent] :as db} :db} [_ type elements]]
    (when stripe
      (let [data       (clj->js
                         {:payment_method
                          {:type            type
                           type             (case type
                                              "sepa_debit" (.getElement elements
                                                                        react-stripe/IbanElement)
                                              "card" (.getElement elements
                                                                  react-stripe/CardElement))
                           :billing_details (when (= type "sepa_debit")
                                              {:name  "test"
                                               :email "test@example.com"})}})
            effect-key (if (= type "card") ::fx/confirm-card-setup ::fx/confirm-sepa-debit-setup)]
        {effect-key [stripe (:client-secret setup-intent)
                     data #(dispatch [::set-confirm-card-setup-result %])]
         :db        (update db ::spec/loading conj :confirm-setup-intent)}))))


(reg-event-fx
  ::set-confirm-card-setup-result
  (fn [{{:keys [::spec/loading
                ::spec/customer] :as db} :db} [_ result]]
    (let [res   (-> result (js->clj :keywordize-keys true))
          error (:error res)]
      (if error
        {:dispatch [::set-error (:message error) :confirm-setup-intent]}
        {:db         (-> db
                         (assoc ::spec/setup-intent nil)
                         (update ::spec/loading disj :confirm-setup-intent))
         :dispatch-n [[::list-payment-methods]
                      [::upcoming-invoice]
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


(reg-event-fx
  ::add-coupon
  (fn [{{:keys [::spec/customer] :as db} :db} [_ coupon]]
    {::cimi-api-fx/operation [(:id customer) "add-coupon"
                              #(dispatch [::add-coupon-result %])
                              {:coupon coupon}]
     :db                     (update db ::spec/loading conj :add-coupon)}))


(reg-event-fx
  ::add-coupon-result
  (fn [{db :db} [_ result]]
    (if (instance? js/Error result)
      {:dispatch [::set-error (-> result response/parse-ex-info :message) :add-coupon]}
      {:dispatch [::customer-info]
       :db       (update db ::spec/loading disj :add-coupon)})))


(reg-event-fx
  ::remove-coupon
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    {::cimi-api-fx/operation [(:id customer) "remove-coupon"
                              #(dispatch [::remove-coupon-result %])]
     :db                     (update db ::spec/loading conj :remove-coupon)}))


(reg-event-fx
  ::remove-coupon-result
  (fn [{db :db} [_ result]]
    (if (instance? js/Error result)
      {:dispatch [::set-error (-> result response/parse-ex-info :message) :remove-coupon]}
      {:dispatch [::customer-info]
       :db       (update db ::spec/loading disj :remove-coupon)})))