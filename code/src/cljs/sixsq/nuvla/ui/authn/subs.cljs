(ns sixsq.nuvla.ui.authn.subs
  (:require
    [clojure.string :as str]
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
    (some-> session :username (str/replace #"user/" ""))))


(reg-sub
  ::error-message
  ::spec/error-message)


(reg-sub
  ::success-message
  ::spec/success-message)


(reg-sub
  ::redirect-uri
  ::spec/redirect-uri)


(reg-sub
  ::server-redirect-uri
  ::spec/server-redirect-uri)


(reg-sub
  ::form-id
  ::spec/form-id)
