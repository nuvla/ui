(ns sixsq.nuvla.ui.profile.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
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
  :<- [::customer]
  (fn [customer]
    (:subscription customer)))


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
  ::stripe
  (fn [db]
    (::spec/stripe db)))

(reg-sub
  ::processing?
  (fn [db]
    (::spec/processing? db)))


(reg-sub
  ::subscribe-button-disabled?
  :<- [::customer]
  :<- [::loading? :customer]
  (fn [[customer loading?]]
    (or
      loading?
      (some? customer))))