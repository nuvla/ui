(ns sixsq.nuvla.ui.authn.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.utils.spec :as us]))


;; VALIDATION SPEC

(s/def ::email ::us/email)
(s/def ::username ::us/username)
(s/def ::password ::us/password)
(s/def ::key ::us/key)
(s/def ::secret ::us/secret)

;;; user-template/email-password

(s/def ::repeat-password ::us/password)

(s/def ::user-template-email-password (s/keys :req-un [::password
                                                       ::repeat-password
                                                       ::email]))

;;; user-template/email-invitation

(s/def ::user-template-email-invitation (s/keys :req-un [::email]))

;;; session-template/api-key

(s/def ::session-template-api-key (s/keys :req-un [::key
                                                   ::secret]))

;;; session-template/password-reset

(s/def ::new-password ::us/password)

(s/def ::repeat-new-password ::us/password)

(s/def ::session-template-password-reset (s/keys :req-un [::new-password
                                                          ::repeat-new-password]))

;;; RE-FRAME-DB SPEC


(s/def ::open-modal (s/nilable #{:login :reset-password :signup}))

(s/def ::selected-method-group (s/nilable any?))

(s/def ::session (s/nilable any?))

(s/def ::current-user-params (s/nilable any?))

(s/def ::error-message (s/nilable string?))

(s/def ::success-message (s/nilable string?))

(s/def ::redirect-uri (s/nilable string?))

(s/def ::server-redirect-uri string?)

(s/def ::form-id (s/nilable string?))

(s/def ::form-data any?)

(s/def ::loading? boolean?)


(s/def ::db (s/keys :req [::open-modal
                          ::selected-method-group
                          ::session
                          ::current-user-params
                          ::error-message
                          ::success-message
                          ::redirect-uri
                          ::server-redirect-uri
                          ::loading?
                          ::form-id
                          ::form-data
                          ::form-spec]))


(def defaults
  {::open-modal            nil
   ::selected-method-group nil
   ::session               nil
   ::current-user-params   nil
   ::error-message         nil
   ::success-message       nil
   ::redirect-uri          nil
   ::server-redirect-uri   (str @config/path-prefix "/welcome")
   ::loading?              false
   ::form-id               nil
   ::form-data             {}})
