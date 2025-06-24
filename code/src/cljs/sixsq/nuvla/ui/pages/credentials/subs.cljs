(ns sixsq.nuvla.ui.pages.credentials.subs
  (:require [re-frame.core :refer [reg-sub reg-event-fx]]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.pages.credentials.spec :as spec]))

(reg-sub
  ::is-new?
  :-> ::spec/is-new?)

(reg-sub
  ::form-valid?
  :-> ::spec/form-valid?)

(reg-sub
  ::validate-form?
  :-> ::spec/validate-form?)

(reg-sub
  ::active-input
  :-> ::spec/active-input)

(reg-sub
  ::credential
  :-> ::spec/credential)

(reg-sub
  ::credentials
  :-> ::spec/credentials)

(reg-sub
  ::credentials-resources
  :<- [::credentials]
  (fn [credentials-response]
    (vec (:resources credentials-response))))


(def credentials-table-col-configs-local-storage-key "nuvla.ui.table.credentials.column-configs")

(reg-sub
  ::table-current-cols
  :<- [::main-subs/current-cols credentials-table-col-configs-local-storage-key ::events-columns-ordering]
  identity)

(reg-sub
  ::credentials-summary
  :-> ::spec/credentials-summary)

(reg-sub
  ::credential-password
  :-> ::spec/credential-password)

(reg-sub
  ::add-credential-modal-visible?
  :-> ::spec/add-credential-modal-visible?)

(reg-sub
  ::infrastructure-services-available
  :-> ::spec/infrastructure-services-available)

(reg-sub
  ::generated-credential-modal
  :-> ::spec/generated-credential-modal)

(reg-sub
  ::credential-modal-visible?
  :-> ::spec/credential-modal-visible?)
