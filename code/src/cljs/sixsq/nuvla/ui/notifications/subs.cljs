(ns sixsq.nuvla.ui.notifications.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.notifications.spec :as spec]))


;;
;; notification-method
;;

(reg-sub
  ::notification-method
  ::spec/notification-method)


(reg-sub
  ::notification-methods
  (fn [db]
    (::spec/notification-methods db)))

(reg-sub
  ::notification-subscription-configs
  (fn [db]
    (::spec/notification-subscription-configs db)))

(reg-sub
  ::add-notification-method-modal-visible?
  (fn [db]
    (::spec/add-notification-method-modal-visible? db)))


(reg-sub
  ::notification-method-modal-visible?
  (fn [db]
    (::spec/notification-method-modal-visible? db)))


;;
;; subscription
;;


(reg-sub
  ::subscription
  ::spec/subscription)


(reg-sub
  ::subscriptions
  (fn [db]
    (::spec/subscriptions db)))


(reg-sub
  ::edit-subscription-modal-visible?
  (fn [db]
    (::spec/edit-subscription-modal-visible? db)))


(reg-sub
  ::notification-subscriptions-modal-visible?
  (fn [db]
    (::spec/notification-subscriptions-modal-visible? db)))


(reg-sub
  ::add-trigger-modal-visible?
  (fn [db]
    (::spec/add-subscription-modal-visible? db)))


(reg-sub
  ::resource-kind
  ::spec/resource-kind)


(reg-sub
  ::resource-tag
  ::spec/resource-tag)


(reg-sub
  ::resource-tags-available
  ::spec/resource-tags-available)


;; Validation

; Is the form valid?

(reg-sub
  ::form-valid?
  ::spec/form-valid?)


; Should the form be validated?

(reg-sub
  ::validate-form?
  ::spec/validate-form?)


(reg-sub
  ::is-new?
  ::spec/is-new?)


