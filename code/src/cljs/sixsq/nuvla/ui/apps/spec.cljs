(ns sixsq.nuvla.ui.apps.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as utils-spec]
    [sixsq.nuvla.ui.utils.spec :as spec-utils]))

;; Utils

(defn nonblank-string [s]
  (utils-spec/nonblank-string s))

;; Module

(s/def ::name nonblank-string)

(s/def ::description nonblank-string)

(s/def ::parent-path any?)

(s/def ::acl any?)

(s/def ::path any?)

(s/def ::logo-url any?)

; Environmental-variables

(def env-var-regex #"^[A-Z_]+$")
(def reserved-env-var-regex #"NUVLA_.*")
(s/def ::env-name (s/and spec-utils/nonblank-string
                         #(re-matches env-var-regex %)
                         #(not (re-matches reserved-env-var-regex %))))

(s/def ::env-description (s/nilable spec-utils/nonblank-string))

(s/def ::env-value (s/nilable spec-utils/nonblank-string))

(s/def ::env-required boolean?)


(s/def ::env-variable
  (s/keys :req [::env-name]
          :opt [::env-description ::env-required ::env-value]))

(s/def ::env-variables (s/map-of any? (s/merge ::env-variable)))

(s/def ::module-common (s/keys :req [::name
                                     ::parent-path
                                     ; needed by the server, but not the ui
                                     ; (this is handled before contacting the server)
                                     ;::path
                                     ]
                               :opt [::description
                                     ::logo-url
                                     ::acl
                                     ::env-variables]))

;; Validation

; Is the form valid?
(s/def ::form-valid? boolean?)

; Should the form be validated?
(s/def ::validate-form? boolean?)

; Spec to use when validating form
(s/def ::form-spec any?)

; TODO: should be set (:component, :project)
(s/def ::module-subtype any?)

;; Page

(s/def ::default-logo-url (s/nilable string?))

(s/def ::logo-url-modal-visible? boolean?)

(s/def ::active-input (s/nilable string?))

(s/def ::version-warning? boolean?)

(s/def ::is-new? boolean?)

(s/def ::completed? boolean?)

(s/def ::save-modal-visible? boolean?)

(s/def ::add-modal-visible? boolean?)

(s/def ::commit-message (s/nilable string?))

(s/def ::db (s/keys :req [::active-input
                          ::form-spec
                          ::form-valid?
                          ::validate-form?
                          ::is-new?
                          ::completed?
                          ::add-modal-visible?
                          ::default-logo-url
                          ::logo-url-modal-visible?
                          ::save-modal-visible?
                          ::commit-message]))

(def defaults {::active-input            nil
               ::form-spec               nil
               ::form-valid?             true
               ::validate-form?          false
               ::version-warning?        false
               ::is-new?                 false
               ::completed?              true
               ::add-modal-visible?      false
               ::logo-url-modal-visible? false
               ::save-modal-visible?     false
               ::default-logo-url        "/ui/images/noimage.png"
               ::commit-message          ""})
