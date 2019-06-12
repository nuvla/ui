(ns sixsq.nuvla.ui.authn.subs
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.authn.spec :as spec]
    [sixsq.nuvla.ui.authn.utils :as utils]
    [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
    [sixsq.nuvla.ui.utils.spec :as us]
    [taoensso.timbre :as log]))


(reg-sub
  ::open-modal
  (fn [db]
    (::spec/open-modal db)))


(reg-sub
  ::selected-method-group
  (fn [db]
    (::spec/selected-method-group db)))


(reg-sub
  ::session
  (fn [db]
    (::spec/session db)))


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


(reg-sub
  ::form-data-when-form-id-is
  (fn [_ [_ id]]
    [(subscribe [::form-id])
     (subscribe [::form-data])])
  (fn [[form-id form-data] [_ id]]
    (when (= form-id id)
      form-data)))


(reg-sub
  ::form-data-signup
  :<- [::form-data-when-form-id-is "user-template/email-password"]
  (fn [form-data]
    form-data))


(reg-sub
  ::form-signup-email-invalid?
  :<- [::form-data-signup]
  (fn [{:keys [email] :as form-data} _]
    (not (s/valid? (s/nilable ::spec/email) email))))


(reg-sub
  ::form-signup-passwords-doesnt-match?
  :<- [::form-data-signup]
  (fn [{:keys [password repeat-password] :as form-data} _]
    (and (some? password)
         (not= password repeat-password))))


(reg-sub
  ::form-signup-password-constraint-error?
  :<- [::form-data-signup]
  (fn [{:keys [password] :as form-data} _]
    (not (s/valid? (s/nilable ::spec/password) password))))


(reg-sub
  ::form-spec-error?
  (fn [_ _]
    [(subscribe [::form-spec])
     (subscribe [::form-data])])
  (fn [[form-spec form-data]]
    (not (s/valid? form-spec form-data))))


(reg-sub
  ::form-signup-valid?
  (fn [_ _]
    [(subscribe [::form-signup-email-invalid?])
     (subscribe [::form-signup-passwords-doesnt-match?])
     (subscribe [::form-signup-password-constraint-error?])
     (subscribe [::form-spec-error?])])
  (fn [errors]
    (every? false? errors)))

(reg-sub
  ::form-data-password-reset
  :<- [::form-data-when-form-id-is "session-template/password-reset"]
  (fn [form-data]
    form-data))


(reg-sub
  ::form-password-reset-username-invalid?
  :<- [::form-data-password-reset]
  (fn [{:keys [username] :as form-data} _]
    (not (s/valid? (s/nilable ::spec/username) username))))


(reg-sub
  ::form-password-reset-passwords-doesnt-match?
  :<- [::form-data-password-reset]
  (fn [{:keys [new-password repeat-new-password] :as form-data} _]
    (and (some? new-password)
         (not= new-password repeat-new-password))))


(reg-sub
  ::form-password-reset-password-constraint-error?
  :<- [::form-data-password-reset]
  (fn [{:keys [new-password] :as form-data} _]
    (not (s/valid? (s/nilable ::spec/new-password) new-password))))


(reg-sub
  ::form-password-reset-valid?
  (fn [_ _]
    [(subscribe [::form-password-reset-username-invalid?])
     (subscribe [::form-password-reset-passwords-doesnt-match?])
     (subscribe [::form-password-reset-password-constraint-error?])
     (subscribe [::form-spec-error?])])
  (fn [errors]
    (every? false? errors)))


(reg-sub
  ::form-valid?
  (fn [_ _]
    [(subscribe [::form-id])
     (subscribe [::form-signup-valid?])])
  (fn [[form-id form-signup-valid?]]
    (case form-id
      "user-template/email-password" form-signup-valid?
      true)))