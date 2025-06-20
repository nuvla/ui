(ns sixsq.nuvla.ui.pages.credentials.subs
  (:require [re-frame.core :refer [reg-sub reg-event-fx]]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.pages.credentials.spec :as spec]))

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
  ::credentials2
  :-> ::spec/credentials2)

(reg-sub
  ::credentials2-resources
  :<- [::credentials2]
  (fn [credentials-response]
    (vec (:resources credentials-response))))


(def credentials-table-col-configs-local-storage-key "nuvla.ui.table.credentials.column-configs")

(reg-sub
  ::table-current-cols
  :<- [::main-subs/current-cols credentials-table-col-configs-local-storage-key ::events-columns-ordering]
  identity)

(reg-sub
  ::credentials-summary
  (fn [db]
    (::spec/credentials-summary db)))

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

(reg-sub
  ::credential-modal-visible?
  (fn [db]
    (::spec/credential-modal-visible? db)))
