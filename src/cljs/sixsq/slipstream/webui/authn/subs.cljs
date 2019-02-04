(ns sixsq.slipstream.webui.authn.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.slipstream.webui.authn.spec :as authn-spec]
    [sixsq.slipstream.webui.authn.utils :as utils]))


(reg-sub
  ::open-modal
  ::authn-spec/open-modal)


(reg-sub
  ::selected-method-group
  ::authn-spec/selected-method-group)


(reg-sub
  ::session
  ::authn-spec/session)


(reg-sub
  ::loading?
  ::authn-spec/loading?)


(reg-sub
  ::current-user-params
  ::authn-spec/current-user-params)


(reg-sub
  ::is-admin?
  :<- [::session]
  (fn [session _]
    (utils/has-role? session "ADMIN")))


(reg-sub
  ::is-user?
  :<- [::session]
  (fn [session _]
    (utils/has-role? session "USER")))


(reg-sub
  ::user
  :<- [::session]
  (fn [session _]
    (some-> session :username (str/replace #"user/" ""))))


(reg-sub
  ::error-message
  ::authn-spec/error-message)


(reg-sub
  ::success-message
  ::authn-spec/success-message)


(reg-sub
  ::redirect-uri
  ::authn-spec/redirect-uri)


(reg-sub
  ::server-redirect-uri
  ::authn-spec/server-redirect-uri)


(reg-sub
  ::form-id
  ::authn-spec/form-id)
