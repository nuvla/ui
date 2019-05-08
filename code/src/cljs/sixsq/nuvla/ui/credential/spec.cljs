(ns sixsq.nuvla.ui.credential.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as utils-spec]))


(s/def ::credential any?)

(s/def ::credentials any?)

(s/def ::credential-password string?)

(s/def ::open-modal (s/nilable #{:change-password}))

(s/def ::error-message (s/nilable string?))

(s/def ::form-data any?)

(s/def ::add-credential-modal-visible? boolean?)

(s/def ::credential-modal-visible? boolean?)

(s/def ::delete-confirmation-modal-visible? boolean?)

(s/def ::credential-form-valid? boolean?)

(s/def ::active-input (s/nilable string?))

;; Validation

; Is the form valid?
(s/def ::form-valid? boolean?)

; Should the form be validated?
(s/def ::validate-form? boolean?)

; Spec to use when validating form
(s/def ::form-spec any?)

; General

(s/def ::name utils-spec/nonblank-string)
(s/def ::description utils-spec/nonblank-string)

; Swarm

(s/def ::ca utils-spec/nonblank-string)
(s/def ::cert utils-spec/nonblank-string)
(s/def ::key utils-spec/nonblank-string)
(s/def ::infrastructure-service-swarm any?)

(s/def ::swarm-credential (s/keys :req-un [::name
                                           ::description
                                           ::ca
                                           ::cert
                                           ::key]
                                  :opt-un [::infrastructure-service-swarm]))


; MinIO

(s/def ::access-key utils-spec/nonblank-string)
(s/def ::secret-key utils-spec/nonblank-string)
(s/def ::infrastructure-service-minio any?)

(s/def ::minio-credential (s/keys :req-un [::name
                                           ::description
                                           ::access-key
                                           ::secret-key]
                                  :opt-un [::infrastructure-service-minio]))


(s/def ::db (s/keys :req [::add-credential-modal-visible?
                          ::credentials-modal-visible?
                          ::delete-confirmation-modal-visible?
                          ::credential
                          ::credentials
                          ::active-input
                          ::form-spec
                          ::form-valid?
                          ::validate-form?
                          ::credential-password
                          ::error-message]))


(def defaults {::add-credential-modal-visible?      false
               ::credential-modal-visible?          false
               ::delete-confirmation-modal-visible? false
               ::credentials                        []
               ::credential                         {}
               ::active-input                       nil
               ::form-spec                          nil
               ::form-valid?                        true
               ::validate-form?                     false
               ::credential-password                nil
               ::error-message                      nil})

