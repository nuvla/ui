(ns sixsq.nuvla.ui.profile.events
  (:require
    ["@stripe/react-stripe-js" :as react-stripe]
    [ajax.core :as ajax]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.spec :as i18n-spec]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.profile.effects :as fx]
    [sixsq.nuvla.ui.profile.spec :as spec]
    [sixsq.nuvla.ui.session.events :as session-events]
    [sixsq.nuvla.ui.session.spec :as session-spec]
    [sixsq.nuvla.ui.session.utils :as session-utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-fx
  ::init
  (fn [{db :db}]
    {:db (merge db spec/defaults)
     :fx [[:dispatch [::get-user]]
          [:dispatch [::search-existing-customer]]]}))

(reg-event-db
  ::add-group-member
  (fn [{:keys [::spec/group] :as db} [_ member]]
    (let [users (:users group)]
      (update-in db [::spec/group :users] #(conj users member)))))

(reg-event-db
  ::remove-group-member
  (fn [{:keys [::spec/group] :as db} [_ member]]
    (let [users (:users group)]
      (update-in db [::spec/group :users] #(vec (disj (set users) member))))))

(reg-event-db
  ::set-user
  (fn [{:keys [::spec/loading] :as db} [_ user]]
    (assoc db ::spec/user user
              ::spec/loading (disj loading :user))))

(reg-event-fx
  ::get-user
  (fn [{{:keys [::session-spec/session] :as db} :db} _]
    (when-let [user (:user session)]
      (let [is-group? (-> session :active-claim session-utils/is-group?)]
        (cond-> {:fx [(when is-group? [:dispatch [::get-group]])]}
                (not is-group?) (assoc ::cimi-api-fx/get [user #(do (dispatch [::set-user %]))]
                                       :db (update db ::spec/loading conj :user)))))))

(reg-event-fx
  ::add-group
  (fn [{_db :db} [_ id name description loading?]]
    (let [user {:template    {:href             "group-template/generic"
                              :group-identifier id}
                :name        name
                :description description}]
      {::cimi-api-fx/add
       ["group" user
        #(let [{:keys [status message resource-id]} (response/parse %)]
           (dispatch [::session-events/search-groups])
           (dispatch [::messages-events/add
                      {:header  (cond-> (str "added " resource-id)
                                        status (str " (" status ")"))
                       :content message
                       :type    :success}])
           (reset! loading? false))]})))

(reg-event-db
  ::set-group
  (fn [{:keys [::spec/loading] :as db} [_ group]]
    (assoc db ::spec/group group
              ::spec/loading (disj loading :group))))

(reg-event-fx
  ::get-group
  (fn [{{:keys [::session-spec/session] :as db} :db} _]
    (when-let [group (:active-claim session)]
      {:db               (update db ::spec/loading conj :group)
       ::cimi-api-fx/get [group #(dispatch [::set-group %])]})))

(reg-event-fx
  ::edit-group
  (fn [_ [_ group]]
    (let [id (:id group)]
      {::cimi-api-fx/edit [id group #(if (instance? js/Error %)
                                       (let [{:keys [status message]} (response/parse-ex-info %)]
                                         (dispatch [::messages-events/add
                                                    {:header  (cond-> "Group update failed"
                                                                      status (str " (" status ")"))
                                                     :content message
                                                     :type    :error}]))
                                       (dispatch [::messages-events/add
                                                  {:header  "Group updated"
                                                   :content "Group updated successfully."
                                                   :type    :info}]))]})))

(reg-event-fx
  ::invite-to-group
  (fn [{db :db} [_ group-id username]]
    (let [on-error   #(let [{:keys [status message]} (response/parse-ex-info %)]
                        (dispatch [::messages-events/add
                                   {:header  (cond-> (str "Invitation to " group-id " for " username "failed!")
                                                     status (str " (" status ")"))
                                    :content message
                                    :type    :error}]))
          on-success #(dispatch [::messages-events/add
                                 {:header  "Invitation successfully sent to user"
                                  :content (str "User will appear in " group-id
                                                " when he accept the invitation sent to his email address.")
                                  :type    :info}])
          data       {:username         username
                      :redirect-url     (str (::session-spec/server-redirect-uri db)
                                             "?message=join-group-accepted")
                      :set-password-url (str @config/path-prefix "/set-password")}]
      {::cimi-api-fx/operation [group-id "invite" on-success :on-error on-error :data data]})))

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
            customer (assoc :fx [(when (general-utils/can-operation? "get-subscription" customer)
                                   [:dispatch [::get-subscription]])
                                 (when (general-utils/can-operation? "customer-info" customer)
                                   [:dispatch [::customer-info]])
                                 (when (general-utils/can-operation? "list-invoices" customer)
                                   [:dispatch [::list-invoices]])
                                 (when (general-utils/can-operation? "upcoming-invoice" customer)
                                   [:dispatch [::upcoming-invoice]])
                                 (when (general-utils/can-operation? "list-payment-methods" customer)
                                   [:dispatch [::list-payment-methods]])]))))

(reg-event-fx
  ::search-existing-customer
  (fn [{{:keys [::session-spec/session]} :db}]
    (if (not= (:active-claim session) "group/nuvla-admin")
      {::cimi-api-fx/search [:customer {:select "id"}
                             #(if-let [id (-> % :resources first :id)]
                                (dispatch [::get-customer id])
                                (do (dispatch [::set-customer nil])
                                    (dispatch [::set-subscription nil])))]}
      {:fx [[:dispatch [::set-customer nil]]
            [:dispatch [::set-subscription nil]]
            [:dispatch [::close-modal]]]})))

(reg-event-fx
  ::get-subscription
  (fn [{{:keys [::spec/customer] :as db} :db}]
    (let [on-success #(dispatch [::set-subscription %])]
      {:db                     (update db ::spec/loading conj :subscription)
       ::cimi-api-fx/operation [(:id customer) "get-subscription" on-success]})))

(reg-event-db
  ::set-subscription
  (fn [{:keys [::spec/loading] :as db} [_ subscription]]
    (assoc db ::spec/subscription subscription
              ::spec/loading (disj loading :subscription))))

(reg-event-fx
  ::customer-info
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    (let [on-success #(dispatch [::set-customer-info %])]
      {:db                     (update db ::spec/loading conj :customer-info)
       ::cimi-api-fx/operation [(:id customer) "customer-info" on-success]})))

(reg-event-db
  ::set-customer-info
  (fn [{:keys [::spec/loading] :as db} [_ customer-info]]
    (assoc db ::spec/customer-info customer-info
              ::spec/loading (disj loading :customer-info))))

(reg-event-fx
  ::list-payment-methods
  (fn [{{:keys [::spec/customer] :as db} :db}]
    (let [on-success #(dispatch [::set-payment-methods %])]
      {:db                     (update db ::spec/loading conj :payment-methods)
       ::cimi-api-fx/operation [(:id customer) "list-payment-methods" on-success]})))

(reg-event-db
  ::set-payment-methods
  (fn [{:keys [::spec/loading] :as db} [_ payment-methods]]
    (assoc db ::spec/payment-methods payment-methods
              ::spec/loading (disj loading :payment-methods))))

(reg-event-fx
  ::upcoming-invoice
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    (let [on-success #(dispatch [::set-upcoming-invoice %])]
      {:db                     (update db ::spec/loading conj :upcoming-invoice)
       ::cimi-api-fx/operation [(:id customer) "upcoming-invoice" on-success]})))

(reg-event-db
  ::set-upcoming-invoice
  (fn [{:keys [::spec/loading] :as db} [_ upcoming-invoice]]
    (assoc db ::spec/upcoming-invoice upcoming-invoice
              ::spec/loading (disj loading :upcoming-invoice))))

(reg-event-fx
  ::list-invoices
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    (let [on-error   #(do (dispatch [::messages-events/add
                                     {:header  "List invoices failed"
                                      :content "Wasn't able to load invoices"
                                      :type    :error}])
                          (dispatch [::set-invoices nil]))
          on-success #(dispatch [::set-invoices %])]
      {:db                     (update db ::spec/loading conj :invoices)
       ::cimi-api-fx/operation [(:id customer) "list-invoices" on-success :on-error on-error]})))

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
    (let [on-success #(let [{:keys [status message]} %]
                        (if (= status 200)
                          (do
                            (dispatch [::close-modal])
                            (dispatch [::messages-events/add
                                       {:header  (str/capitalize (tr [:success]))
                                        :content (str/capitalize (tr [:password-updated]))
                                        :type    :success}]))
                          (dispatch [::set-error (str message " (" status ")")])))
          on-error   #(dispatch [::set-error (-> % response/parse-ex-info :message)])]
      {::cimi-api-fx/operation [(:credential-password user) "change-password" on-success
                                :on-error on-error :data body]})))

(reg-event-fx
  ::create-customer
  (fn [{db :db} [_ {:keys [payment-method] :as customer}]]
    {:db               (update db ::spec/loading conj :create-customer)
     ::cimi-api-fx/add [:customer (cond-> (assoc customer :subscription? true)
                                          payment-method (assoc :payment-method payment-method))
                        #(do
                           (dispatch [::get-customer (:resource-id %)])
                           (dispatch [::close-modal])
                           (dispatch [::history-events/navigate "profile"]))
                        :on-error #(dispatch [::set-error (-> % response/parse-ex-info :message)
                                              :create-customer])]}))

(reg-event-fx
  ::create-subscription
  (fn [{{:keys [::spec/customer] :as db} :db} _]
    (let [on-error   #(dispatch [::set-error (-> % response/parse-ex-info :message)
                                 :create-customer])
          on-success #(do
                        (dispatch [::get-customer (:id customer)])
                        (dispatch [::close-modal])
                        (dispatch [::history-events/navigate "profile"]))]
      {:db                     (update db ::spec/loading conj :create-customer)
       ::cimi-api-fx/operation [(:id customer) "create-subscription" on-success :on-error on-error]})))

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
  (fn [{db :db} [_ result]]
    (let [res            (-> result (js->clj :keywordize-keys true))
          error          (:error res)
          payment-method (get-in res [:setupIntent :payment_method])]
      (if error
        {:dispatch [::set-error (:message error) :confirm-setup-intent]}
        {:db         (-> db
                         (assoc ::spec/setup-intent nil)
                         (update ::spec/loading disj :confirm-setup-intent))
         :dispatch-n [[::set-default-payment-method payment-method]
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
    (let [on-success #(dispatch [::set-setup-intent %])]
      {:db                     (update db ::spec/loading conj :create-setup-intent)
       ::cimi-api-fx/operation [(:id customer) "create-setup-intent" on-success]})))

(reg-event-fx
  ::detach-payment-method
  (fn [{{:keys [::spec/customer]} :db} [_ payment-method]]
    (let [on-success #(dispatch [::list-payment-methods])
          data       {:payment-method payment-method}]
      {::cimi-api-fx/operation [(:id customer) "detach-payment-method" on-success :data data]})))

(reg-event-fx
  ::set-default-payment-method
  (fn [{{:keys [::spec/customer]} :db} [_ payment-method]]
    (let [on-success #(dispatch [::list-payment-methods])
          data       {:payment-method payment-method}]
      {::cimi-api-fx/operation [(:id customer) "set-default-payment-method" on-success :data data]})))

(reg-event-fx
  ::add-coupon
  (fn [{{:keys [::spec/customer] :as db} :db} [_ coupon]]
    (let [on-success #(dispatch [::add-coupon-result %])
          data       {:coupon coupon}]
      {::cimi-api-fx/operation [(:id customer) "add-coupon" on-success :data data]
       :db                     (update db ::spec/loading conj :add-coupon)})))

(reg-event-fx
  ::add-coupon-result
  (fn [{db :db} [_ result]]
    (if (instance? js/Error result)
      {:dispatch [::set-error (-> result response/parse-ex-info :message) :add-coupon]}
      {:fx [[:dispatch [::get-subscription]]
            [:dispatch [::close-modal]]]
       :db (update db ::spec/loading disj :add-coupon)})))

(reg-event-fx
  ::remove-coupon
  (fn [{{:keys [::spec/customer] :as db} :db}]
    (let [on-success #(dispatch [::remove-coupon-result %])]
      {::cimi-api-fx/operation [(:id customer) "remove-coupon" on-success]
       :db                     (update db ::spec/loading conj :remove-coupon)})))

(reg-event-fx
  ::remove-coupon-result
  (fn [{db :db} [_ result]]
    (if (instance? js/Error result)
      {:dispatch [::set-error (-> result response/parse-ex-info :message) :remove-coupon]}
      {:dispatch [::get-subscription]
       :db       (update db ::spec/loading disj :remove-coupon)})))


(reg-event-db
  ::set-vendor
  (fn [{:keys [::spec/loading] :as db} [_ vendor]]
    (assoc db ::spec/vendor vendor
              ::spec/loading (disj loading :vendor))))

(reg-event-fx
  ::get-vendor
  (fn [{db :db} [_ id]]
    {:db               (-> db
                           (update ::spec/loading conj :vendor))
     ::cimi-api-fx/get [id #(dispatch [::set-vendor %])]}))

(reg-event-fx
  ::search-existing-vendor
  (fn [{{:keys [::session-spec/session]} :db}]
    {::cimi-api-fx/search [:vendor {:filter (str "parent='" (or (:active-claim session)
                                                                (:user session)) "'")}
                           #(if-let [id (-> % :resources first :id)]
                              (dispatch [::get-vendor id])
                              (dispatch [::set-vendor nil]))]}))

(reg-event-fx
  ::code-validation-2fa-failed
  (fn [_ [_ response]]
    {:fx [[:dispatch [::set-error (-> response :response :message)]]]}))

(reg-event-fx
  ::code-validation-2fa-success
  (fn [_ [_ success-header success-content]]
    {:fx [[:dispatch [::close-modal]]
          [:dispatch [::messages-events/add
                      {:header  success-header
                       :content success-content
                       :type    :success}]]
          [:dispatch [::get-user]]]}))

(reg-event-fx
  ::two-factor-enabled
  (fn [_ [_ success-header success-content show-confirmation]]
    {:fx [[:dispatch [::messages-events/add
                      {:header  success-header
                       :content success-content
                       :type    :success}]]
          [:dispatch [::get-user]]
          [:dispatch (if show-confirmation
                       [::set-two-factor-step :save-secret]
                       [::close-modal])]]}))

(reg-event-fx
  ::two-factor-disabled
  (fn [_ [_ success-header success-content]]
    {:fx [[:dispatch [::messages-events/add
                      {:header  success-header
                       :content success-content
                       :type    :success}]]
          [:dispatch [::get-user]]
          [:dispatch [::close-modal]]]}))

(reg-event-fx
  ::two-factor-auth-callback-exec
  (fn [{{:keys [::spec/two-factor-callback]} :db} [_ token success-dispatch-vec]]
    {:http-xhrio {:method          :put
                  :uri             two-factor-callback
                  :format          (ajax/json-request-format)
                  :params          {:token token}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      success-dispatch-vec
                  :on-failure      [::code-validation-2fa-failed]}}))

(reg-event-db
  ::set-two-factor-step
  (fn [db [_ step]]
    (assoc db ::spec/two-factor-step step)))

(reg-event-fx
  ::set-two-factor-op-response
  (fn [{{:keys [::spec/two-factor-enable?] :as db} :db} [_ {:keys [location secret] :as _response}]]
    (let [next-step (if secret :totp :email)]
      {:db (-> db
               (update ::spec/loading disj :two-factor-auth)
               (assoc ::spec/two-factor-callback location)
               (assoc ::spec/two-factor-secret secret))
       :fx [(when two-factor-enable?
              [:dispatch [::set-two-factor-step next-step]])]})))

(reg-event-fx
  ::two-factor-operation-call
  (fn [{{:keys [::spec/user
                ::spec/two-factor-enable?
                ::spec/two-factor-method] :as db} :db}]
    (let [op             (if two-factor-enable? "enable-2fa" "disable-2fa")
          enable-disable (if two-factor-enable? "Enable" "Disable")
          on-error       #(let [{:keys [status message]} (response/parse-ex-info %)]
                            (dispatch [::set-two-factor-op-response nil])
                            (dispatch [::close-modal])
                            (dispatch [::messages-events/add
                                       {:header  (cond-> (str enable-disable " 2FA failed!")
                                                         status (str " (" status ")"))
                                        :content message
                                        :type    :error}]))
          on-success     #(dispatch [::set-two-factor-op-response %1])
          data           {:method two-factor-method}]
      {:db                     (update db ::spec/loading conj :two-factor-auth)
       ::cimi-api-fx/operation [(:id user) op on-success :on-error on-error :data data]})))

(reg-event-fx
  ::select-method
  (fn [{db :db} [_ method]]
    {:db (assoc db ::spec/two-factor-method method)
     :fx [[:dispatch [::two-factor-operation-call]]]}))

(reg-event-db
  ::two-factor-enable
  (fn [db]
    (-> db
        (assoc ::spec/open-modal :two-factor-auth)
        (assoc ::spec/two-factor-step :select-method)
        (assoc ::spec/two-factor-enable? true))))

(reg-event-fx
  ::two-factor-disable
  (fn [{{:keys [::spec/user] :as db} :db}]
    {:db (-> db
             (assoc ::spec/open-modal :two-factor-auth)
             (assoc ::spec/two-factor-enable? false)
             (assoc ::spec/two-factor-step :disable)
             (assoc ::spec/two-factor-method (:auth-method-2fa user)))
     :fx [[:dispatch [::two-factor-operation-call]]]}))
