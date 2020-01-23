(ns sixsq.nuvla.ui.credentials.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.credentials.spec :as spec]))


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
  ::credential
  ::spec/credential)


(reg-sub
  ::credentials
  (fn [db]
    (::spec/credentials db)))


(reg-sub
  ::credential-password
  (fn [db]
    (::spec/credential-password db)))


(reg-sub
  ::add-credential-modal-visible?
  (fn [db]
    (::spec/add-credential-modal-visible? db)))


(reg-sub
  ::infrastructure-services-available
  (fn [db]
    (::spec/infrastructure-services-available db)))


(reg-sub
  ::generated-credential-modal
  (fn [db]
    (::spec/generated-credential-modal db)))
