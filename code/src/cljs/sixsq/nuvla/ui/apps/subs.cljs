(ns sixsq.nuvla.ui.apps.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.apps.spec :as spec]))


(reg-sub
  ::module-common
  ::spec/module-common)


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
  ::version-warning?
  ::spec/version-warning?)


(reg-sub
  ::completed?
  ::spec/completed?)


(reg-sub
  ::is-new?
  ::spec/is-new?)


(reg-sub

  ::module
  ::spec/module)


(reg-sub
  ::add-modal-visible?
  ::spec/add-modal-visible?)


(reg-sub
  ::add-data
  ::spec/add-data)


(reg-sub
  ::save-modal-visible?
  ::spec/save-modal-visible?)


(reg-sub
  ::default-logo-url
  ::spec/default-logo-url)


(reg-sub
  ::logo-url-modal-visible?
  ::spec/logo-url-modal-visible?)

(reg-sub
  ::commit-message
  ::spec/commit-message)

