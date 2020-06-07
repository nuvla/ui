(ns sixsq.nuvla.ui.profile.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.profile.spec :as spec]))


(reg-sub
  ::user
  (fn [db]
    (::spec/user db)))


(reg-sub
  ::credential-password
  :<- [::user]
  (fn [user]
    (:credential-password user)))


(reg-sub
  ::customer
  (fn [db]
    (::spec/customer db)))


(reg-sub
  ::subscription
  (fn [db]
    (::spec/subscription db)))


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
  ::default-payment-method
  :<- [::payment-methods]
  (fn [payment-methods]
    (:default-payment-method payment-methods)))


(reg-sub
  ::upcoming-invoice
  (fn [db]
    (::spec/upcoming-invoice db)))


(reg-sub
  ::upcoming-invoice-lines
  :<- [::upcoming-invoice]
  (fn [upcoming-invoice]
    (->> upcoming-invoice
         :lines
         (group-by :period)
         (sort-by #(-> % first :start)))))


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
