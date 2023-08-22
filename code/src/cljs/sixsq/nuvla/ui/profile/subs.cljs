(ns sixsq.nuvla.ui.profile.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.profile.spec :as spec]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(reg-sub
  ::group-trees
  (fn [db]
    (::spec/group-trees db)))

(reg-sub
  ::user
  (fn [db]
    (::spec/user db)))

(reg-sub
  ::customer
  (fn [db]
    (::spec/customer db)))

(reg-sub
  ::subscription
  (fn [db]
    (::spec/subscription db)))

(reg-sub
  ::subscription-canceled?
  :<- [::subscription]
  (fn [{:keys [status]}]
    (= status "canceled")))

(reg-sub
  ::show-subscription
  :<- [::main-subs/stripe]
  :<- [::session-subs/session]
  :<- [::session-subs/is-admin?]
  :<- [::session-subs/is-subgroup?]
  :<- [::customer]
  (fn [[stripe session is-admin? is-subgroup?]]
    (and stripe
         session
         (not is-admin?)
         (not is-subgroup?))))

(reg-sub
  ::show-coupon
  :<- [::customer]
  (fn [customer]
    (general-utils/can-operation? "add-coupon" customer)))

(reg-sub
  ::show-billing-contact
  :<- [::customer]
  (fn [customer]
    (general-utils/can-operation? "customer-info" customer)))

(reg-sub
  ::show-consumption
  :<- [::customer]
  (fn [customer]
    (general-utils/can-operation? "upcoming-invoice" customer)))

(reg-sub
  ::show-invoices
  :<- [::customer]
  (fn [customer]
    (general-utils/can-operation? "list-invoices" customer)))

(reg-sub
  ::show-payment-methods
  :<- [::customer]
  (fn [customer]
    (general-utils/can-operation? "list-payment-methods" customer)))

(reg-sub
  ::payment-methods
  (fn [db]
    (::spec/payment-methods db)))

(reg-sub
  ::cards-bank-accounts
  :<- [::payment-methods]
  (fn [payment-methods]
    (let [{:keys [cards bank-accounts]} payment-methods]
      (->> (concat cards (map #(assoc % :brand "iban") bank-accounts))
           (sort-by (juxt :exp-year :exp-month :payment-method))
           seq))))

(reg-sub
  ::payment-methods?
  :<- [::payment-methods]
  (fn [payment-methods]
    (let [{:keys [cards bank-accounts]} payment-methods]
      (boolean
        (or (seq cards)
            (seq bank-accounts))))))

(reg-sub
  ::default-payment-method
  :<- [::payment-methods]
  (fn [payment-methods]
    (:default-payment-method payment-methods)))

(reg-sub
  ::upcoming-invoice
  (fn [db]
    (::spec/upcoming-invoice db)))

(defn- calc-upcoming-invoices
  [upcoming-invoice]
  (->> upcoming-invoice
       :lines
       (group-by :period)
       (sort-by #(-> % first :start))))


(reg-sub
  ::upcoming-invoice-lines
  :<- [::upcoming-invoice]
  (fn [upcoming-invoice]
    (calc-upcoming-invoices upcoming-invoice)))

(reg-sub
  ::app-subscriptions
  :-> ::spec/app-subscriptions)

(reg-sub
  ::app-subscriptions-list
  :<- [::app-subscriptions]
  (fn [app-subs]
    (sort-by :sort-order (vals app-subs))))

(reg-sub
  ::apps-subscriptions-consumptions
  :<- [::app-subscriptions-list]
  (fn [app-subs _]
    (map (fn [{:keys [upcoming-invoice] :as sub}]
           {:app-name         (-> sub :metadata :application)
            :subscription     sub
            :upcoming-invoice upcoming-invoice
            :upcoming-lines   (calc-upcoming-invoices upcoming-invoice)})
         app-subs)))

(reg-sub
  ::invoices
  (fn [db]
    (::spec/invoices db)))

(reg-sub
  ::customer-info
  (fn [db]
    (::spec/customer-info db)))

(reg-sub
  ::coupon
  :<- [::customer-info]
  (fn [customer-info]
    (:coupon customer-info)))

(reg-sub
  ::open-modal
  (fn [db]
    (::spec/open-modal db)))

(reg-sub
  ::modal-open?
  :<- [::open-modal]
  (fn [open-modal [_ modal-key]]
    (= open-modal modal-key)))

(reg-sub
  ::error-message
  (fn [db]
    (::spec/error-message db)))

(reg-sub
  ::loading
  (fn [db]
    (::spec/loading db)))

(reg-sub
  ::loading?
  :<- [::loading]
  (fn [loading [_ loading-key]]
    (contains? loading loading-key)))

(reg-sub
  ::cannot-create-setup-intent?
  :<- [::customer]
  (fn [customer]
    (not (general-utils/can-operation? "create-setup-intent" customer))))

(reg-sub
  ::vendor
  (fn [db]
    (::spec/vendor db)))

(reg-sub
  ::group
  (fn [db]
    (::spec/group db)))

(reg-sub
  ::two-factor-step
  (fn [db]
    (::spec/two-factor-step db)))

(reg-sub
  ::two-factor-method
  (fn [db]
    (::spec/two-factor-method db)))

(reg-sub
  ::two-factor-secret
  (fn [db]
    (::spec/two-factor-secret db)))

(reg-sub
  ::two-factor-qrcode-value
  :<- [::two-factor-secret]
  :<- [::session-subs/identifier]
  (fn [[secret identifier]]
    (js/encodeURI
      (str "otpauth://totp/"
           @cimi-fx/NUVLA_URL
           ":"
           identifier
           "?secret="
           secret
           "&issuer=Nuvla"))))

(reg-sub
  ::two-factor-enable?
  (fn [db]
    (::spec/two-factor-enable? db)))
