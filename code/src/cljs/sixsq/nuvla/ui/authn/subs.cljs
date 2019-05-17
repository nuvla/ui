(ns sixsq.nuvla.ui.authn.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.authn.spec :as spec]
    [sixsq.nuvla.ui.authn.utils :as utils]))


(reg-sub
  ::open-modal
  ::spec/open-modal)


(reg-sub
  ::selected-method-group
  ::spec/selected-method-group)


(reg-sub
  ::session
  ::spec/session)


(reg-sub
  ::loading?
  ::spec/loading?)


(reg-sub
  ::is-admin?
  :<- [::session]
  (fn [session _]
    (utils/has-role? session "group/nuvla-admin")))


(reg-sub
  ::is-user?
  :<- [::session]
  (fn [session _]
    (utils/has-role? session "group/nuvla-user")))


(reg-sub
  ::user
  :<- [::session]
  (fn [session _]
    (:identifier session)))


(reg-sub
  ::user-id
  :<- [::session]
  (fn [session _]
    (:user session)))


(reg-sub
  ::error-message
  ::spec/error-message)


(reg-sub
  ::success-message
  ::spec/success-message)


(reg-sub
  ::form-id
  ::spec/form-id)


(reg-sub
  ::form-data
  (fn [db]
    (::spec/form-data db)))


(reg-sub
  ::fields-in-errors
  :<- [::form-id]
  :<- [::form-data]
  (fn [[form-id form-data]]
    (let [errors (case form-id
                   "user-template/email-password" [(when-not
                                                     (= (:password form-data)
                                                        (:repeat-password form-data))
                                                     "password")
                                                   (when (empty? (:password form-data))
                                                     "error")
                                                   (when (:email form-data)
                                                     "error")]
                   "user-template/email-invitation" [(when-not
                                                       (:email form-data)
                                                       "password")]
                   "session-template/password" [(when-not (and (:username form-data)
                                                               (:password form-data))
                                                  "error")]
                   "session-template/api-key" [(when-not (and (:key form-data)
                                                              (:secret form-data))
                                                 "error")]
                   "session-template/password-reset" [(when-not
                                                        (= (:new-password form-data)
                                                           (:repeat-new-password form-data))
                                                        "password")
                                                      (when (empty? (:new-password form-data))
                                                        "error")
                                                      (when (empty? (:username form-data))
                                                        "error")]
                   nil ["error"]
                   #{})]
      (->> errors seq (remove nil?) set))))


(reg-sub
  ::form-error?
  :<- [::fields-in-errors]
  (fn [fields-in-errors]
    (some? (seq fields-in-errors))))


(reg-sub
  ::server-redirect-uri
  (fn [db]
    (::spec/server-redirect-uri db)))
