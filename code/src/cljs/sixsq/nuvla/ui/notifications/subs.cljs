(ns sixsq.nuvla.ui.notifications.subs
  (:require
    [re-frame.core :refer [reg-sub]]
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
  ::add-notification-method-modal-visible?
  (fn [db]
    (::spec/add-notification-method-modal-visible? db)))


(reg-sub
  ::notification-method-modal-visible?
  (fn [db]
    (::spec/notification-method-modal-visible? db)))


(reg-sub
  ::notification-method-create-button-visible?
  (fn [db]
    (::spec/notification-method-create-button-visible? db)))

;;
;; subscription configuration
;;

(reg-sub
  ::notification-subscription-configs
  (fn [db]
    (::spec/notification-subscription-configs db)))

(reg-sub
  ::notification-subscription-config
  (fn [db]
    (::spec/notification-subscription-config db)))

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
  ::subscriptions-by-parent
  (fn [db]
    (::spec/subscriptions-by-parent db)))


(reg-sub
  ::subscriptions-by-parent-counts
  (fn [db]
    (::spec/subscriptions-by-parent-counts db)))


(reg-sub
  ::subscriptions-for-parent
  (fn [db]
    (::spec/subscriptions-for-parent db)))


(reg-sub
  ::notification-subscriptions
  (fn [db]
    (::spec/notification-subscriptions db)))


(reg-sub
  ::notification-subscription-config-id
  (fn [db]
    (::spec/notification-subscription-config-id db)))

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
  ::subscription-config-modal-visible?
  (fn [db]
    (::spec/add-subscription-config-modal-visible? db)))


(reg-sub
  ::edit-subscription-config-modal-visible?
  (fn [db]
    (::spec/edit-subscription-config-modal-visible? db)))


(reg-sub
  ::collection
  (fn [db]
    (get-in db [::spec/notification-subscription-config :collection])))


(reg-sub
  ::resource-tag
  ::spec/resource-tag)


(reg-sub
  ::resource-tags-available
  ::spec/resource-tags-available)


(reg-sub
  ::components-number
  (fn [db]
    (get db ::spec/components-number)))



(reg-sub
  ::criteria-metric
  (fn [db]
    (get-in db [::spec/notification-subscription-config :criteria :metric])))


;; Validation

; Is the form valid?

(reg-sub
  ::form-valid?
  ::spec/form-valid?)


; Should the form be validated?

(reg-sub
  ::validate-form?
  (fn [db]
    (::spec/validate-form? db)))


(reg-sub
  ::is-new?
  ::spec/is-new?)


