(ns sixsq.nuvla.ui.apps.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as spec-utils]))

;; Utils

(defn nonblank-string [s]
  (spec-utils/nonblank-string s))

;; Module

(s/def ::module any?)

(s/def ::version number?)

(s/def ::name nonblank-string)

(s/def ::description nonblank-string)

(s/def ::parent-path any?)

(s/def ::acl any?)

(s/def ::path any?)

(s/def ::logo-url any?)

; Environmental-variables

(def env-var-regex #"^[a-zA-Z_]+[a-zA-Z0-9_]*$")
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

; URLs

(s/def ::url-name spec-utils/nonblank-string)

(s/def ::url spec-utils/nonblank-string)

(s/def ::single-url (s/keys :req [::url-name
                                  ::url]))

(s/def ::urls (s/map-of any? (s/merge ::single-url)))

; Output parameters

(s/def ::output-parameter-name spec-utils/nonblank-string)

(s/def ::output-parameter-description spec-utils/nonblank-string)

(s/def ::output-parameter (s/keys :req [::output-parameter-name
                                        ::output-parameter-description]))

(s/def ::output-parameters (s/map-of any? (s/merge ::output-parameter)))

; Data types

(s/def ::data-type string?)


(s/def ::data-type-map (s/keys :req [::data-type]))

(s/def ::data-types (s/map-of any? (s/merge ::data-type-map)))

(s/def ::registry-id spec-utils/nonblank-string)

(s/def ::registry-cred-id string?)

(s/def ::single-registry (s/keys :req [::registry-cred-id
                                       ::registry-id]))

(s/def ::registries (s/map-of any? (s/merge ::single-registry)))

(s/def ::cent-amount-daily (s/nilable pos-int?))

(s/def ::currency string?)

(s/def ::price (s/nilable (s/keys :req-un [::cent-amount-daily
                                           ::currency])))

(s/def ::license-name spec-utils/nonblank-string)

(s/def ::license-description (s/nilable string?))

(s/def ::license-url spec-utils/nonblank-string)

(s/def ::license (s/nilable (s/keys :req-un [::license-name
                                             ::license-url]
                                    :opt-un [::license-description])))

(s/def ::module-common (s/keys :req [::name
                                     ::description
                                     ::parent-path
                                     ::license
                                     ; needed by the server, but not the ui
                                     ; (this is handled before contacting the server)
                                     ;::path
                                     ]
                               :opt [::logo-url
                                     ::acl
                                     ::env-variables
                                     ::urls
                                     ::output-parameters
                                     ::data-types
                                     ::price]))


;; Validation

; Is the form valid?
(s/def ::form-valid? boolean?)

; Should the form be validated?
(s/def ::validate-form? boolean?)

; Spec to use when validating form
(s/def ::form-spec any?)

(s/def ::details-validation-errors set?)

;; Page

(s/def ::default-logo-url (s/nilable string?))

(s/def ::logo-url-modal-visible? boolean?)

(s/def ::active-input (s/nilable string?))

(s/def ::is-new? boolean?)

(s/def ::completed? boolean?)

(s/def ::save-modal-visible? boolean?)

(s/def ::add-modal-visible? boolean?)

(s/def ::commit-message (s/nilable string?))

(s/def ::registries-infra any?)

(s/def ::registries-credentials any?)

(s/def ::validate-docker-compose any?)

(s/def ::compare-module-left any?)

(s/def ::compare-module-right any?)

(s/def ::copy-module any?)

(s/def ::paste-modal-visible? boolean?)

(s/def ::active-tab-index number?)

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
                          ::commit-message
                          ::registries-infra
                          ::registries-credentials
                          ::validate-docker-compose
                          ::compare-module-left
                          ::compare-module-right
                          ::module
                          ::version
                          ::copy-module
                          ::paste-modal-visible?
                          ::active-tab-index]))

(def defaults {::active-input              nil
               ::form-spec                 nil
               ::form-valid?               true
               ::validate-form?            false
               ::is-new?                   false
               ::completed?                true
               ::add-modal-visible?        false
               ::logo-url-modal-visible?   false
               ::save-modal-visible?       false
               ::default-logo-url          "/ui/images/noimage.png"
               ::commit-message            ""
               ::registries                nil
               ::registries-infra          nil
               ::registries-credentials    nil
               ::validate-docker-compose   nil
               ::compare-module-left       nil
               ::compare-module-right      nil
               ::module                    nil
               ::version                   nil
               ::copy-module               nil
               ::paste-modal-visible?      false
               ::active-tab-index          0
               ::details-validation-errors #{}})
