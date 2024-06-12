(ns sixsq.nuvla.ui.pages.clouds.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.pages.clouds.spec :as spec]))

(reg-sub
  ::infra-service-groups
  ::spec/infra-service-groups)

(reg-sub
  ::infra-services
  (fn [db]
    (::spec/infra-services db)))

(reg-sub
  ::services-in-group
  :<- [::infra-services]
  (fn [services [_ group-id]]
    (-> services
        :groups
        (get group-id))))

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
  ::infra-service
  ::spec/infra-service)

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
