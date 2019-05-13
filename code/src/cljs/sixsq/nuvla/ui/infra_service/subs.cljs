(ns sixsq.nuvla.ui.infra-service.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.infra-service.spec :as spec]))


(reg-sub
  ::services
  ::spec/services)


(reg-sub
  ::elements-per-page
  ::spec/elements-per-page)


(reg-sub
  ::page
  ::spec/page)


(reg-sub
  ::is-new?
  ::spec/is-new?)


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
  ::active-input
  ::spec/active-input)


(reg-sub
  ::service
  ::spec/service)


(reg-sub
  ::service-group
  ::spec/service-group)


(reg-sub
  ::service-modal-visible?
  (fn [db]
    (::spec/service-modal-visible? db)))


(reg-sub
  ::add-service-modal-visible?
  (fn [db]
    (::spec/add-service-modal-visible? db)))

