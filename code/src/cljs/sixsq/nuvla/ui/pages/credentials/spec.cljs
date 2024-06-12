(ns sixsq.nuvla.ui.pages.credentials.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.utils.spec :as utils-spec]))


(s/def ::credential any?)

(s/def ::credentials any?)

(s/def ::credentials-summary any?)

(s/def ::credential-password string?)

(s/def ::add-credential-modal-visible? boolean?)

(s/def ::generated-credential-modal any?)

(s/def ::credential-modal-visible? boolean?)

(s/def ::active-input (s/nilable string?))

(s/def ::credential-check-table any?)

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
(s/def ::parent utils-spec/nonblank-string)

; COE

(s/def ::ca utils-spec/nonblank-string)
(s/def ::cert utils-spec/nonblank-string)
(s/def ::key utils-spec/nonblank-string)

(s/def ::coe-credential (s/keys :req-un [::name
                                         ::description
                                         ::ca
                                         ::cert
                                         ::key
                                         ::parent]))

; Keypair

(s/def ::public-key utils-spec/nonblank-string)
(s/def ::public-key-optional (s/nilable string?))
(s/def ::private-key-optional (s/nilable string?))

; SSH

(s/def ::ssh-credential (s/keys :req-un [::name
                                         ::description]
                                :opt-un [::public-key-optional
                                         ::private-key-optional]))

; GPG

(s/def ::gpg-credential (s/keys :req-un [::name
                                         ::description
                                         ::public-key]
                                :opt-un [::private-key-optional]))

; MinIO

(s/def ::access-key utils-spec/nonblank-string)
(s/def ::secret-key utils-spec/nonblank-string)

(s/def ::minio-credential (s/keys :req-un [::name
                                           ::description
                                           ::access-key
                                           ::secret-key
                                           ::parent]))


; Registry

(s/def ::username utils-spec/nonblank-string)
(s/def ::password utils-spec/nonblank-string)

(s/def ::registry-credential (s/keys :req-un [::name
                                              ::description
                                              ::username
                                              ::password
                                              ::parent]))


(s/def ::api-key-credential (s/keys :req-un [::name
                                             ::description]))


; VPN

(s/def ::vpn-credential (s/keys :req-un [::name
                                         ::description
                                         ::parent]))


(s/def ::state-selector any?)

(s/def ::infrastructure-services-available any?)

(s/def ::tab any?)

(def defaults {::add-credential-modal-visible?     false
               ::credential-modal-visible?         false
               ::generated-credential-modal        nil
               ::credentials                       []
               ::credentials-summary               []
               ::credential                        {}
               ::active-input                      nil
               ::form-spec                         nil
               ::form-valid?                       true
               ::validate-form?                    false
               ::credential-password               nil
               ::error-message                     nil
               ::state-selector                    nil
               ::infrastructure-services-available nil
               ::credential-check-table            nil})
