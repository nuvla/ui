(ns sixsq.nuvla.ui.profile.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.profile.spec :as spec]))


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
  (fn [db]
    (::spec/credential db)))


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
  ::credential-modal-visible?
  (fn [db]
    (::spec/credential-modal-visible? db)))


; TODO: make more specific
(reg-sub
  ::open-modal
  ::spec/open-modal)


(reg-sub
  ::error-message
  ::spec/error-message)


(reg-sub
  ::form-data
  (fn [db]
    (::spec/form-data db)))


(reg-sub
  ::fields-in-errors
  :<- [::form-data]
  (fn [form-data]
    (let [errors [(when-not
                    (= (:new-password form-data)
                       (:repeat-new-password form-data))
                    "password")
                  (when (empty? (:current-password form-data))
                    "error")
                  (when (empty? (:new-password form-data))
                    "error")]]
      (->> errors seq (remove nil?) set))))


(reg-sub
  ::form-error?
  :<- [::fields-in-errors]
  (fn [fields-in-errors]
    (some? (seq fields-in-errors))))