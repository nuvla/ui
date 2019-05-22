(ns sixsq.nuvla.ui.authn.subs
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.authn.spec :as spec]
    [sixsq.nuvla.ui.authn.utils :as utils]
    [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
    [sixsq.nuvla.ui.cimi.utils :as api-utils]
    [sixsq.nuvla.ui.utils.spec :as us]
    [taoensso.timbre :as log]))


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
  ::form-spec
  (fn [{:keys [::spec/form-id] :as db}]

    (or (when form-id
          (when-let [spec-key (some->> (str/replace form-id #"/" "-")
                                       (str 'sixsq.nuvla.ui.authn.spec "/")
                                       (keyword))]
            (when (us/resolvable? spec-key)
              spec-key)))
        any?)))


(reg-sub
  ::server-redirect-uri
  (fn [db]
    (::spec/server-redirect-uri db)))


(reg-sub
  ::user-templates
  :<- [::cimi-subs/collection-templates :user-template]
  (fn [user-templates _]
    user-templates))


(reg-sub
  ::user-template-exist?
  :<- [::user-templates]
  (fn [user-templates [_ template-id]]
    (contains? user-templates template-id)))


(reg-sub
  ::session-templates
  :<- [::cimi-subs/collection-templates :session-template]
  (fn [session-templates _]
    session-templates))


(reg-sub
  ::session-template-exist?
  :<- [::session-templates]
  (fn [session-templates [_ template-id]]
    (contains? session-templates template-id)))