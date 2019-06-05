(ns sixsq.nuvla.ui.profile.subs
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.profile.spec :as spec]))


(reg-sub
  ::credential-password
  (fn [db]
    (::spec/credential-password db)))


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
  ::form-current-password-error?
  :<- [::form-data]
  (fn [{:keys [current-password] :as form-data} _]
    (not (s/valid? (s/nilable ::spec/current-password) current-password))))


(reg-sub
  ::form-passwords-doesnt-match?
  :<- [::form-data]
  (fn [{:keys [new-password repeat-new-password] :as form-data} _]
    (and (some? new-password)
         (not= new-password repeat-new-password))))


(reg-sub
  ::form-password-constraint-error?
  :<- [::form-data]
  (fn [{:keys [new-password] :as form-data} _]
    (not (s/valid? (s/nilable ::spec/new-password) new-password))))


(reg-sub
  ::form-spec-error?
  :<- [::form-data]
  (fn [form-data]
    (not (s/valid? ::spec/change-password-form form-data))))
