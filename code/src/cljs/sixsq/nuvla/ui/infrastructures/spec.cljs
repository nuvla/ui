(ns sixsq.nuvla.ui.infrastructures.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as utils-spec]))

(s/def ::infra-service-groups any?)

(s/def ::infra-services any?)

(s/def ::infra-service any?)

(s/def ::page int?)

(s/def ::elements-per-page int?)

(s/def ::service-modal-visible? boolean?)

(s/def ::add-service-modal-visible? boolean?)

(s/def ::is-new? boolean?)

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

; Service group

(s/def ::documentation utils-spec/nonblank-string)

(s/def ::service-group (s/keys :req-un [::name
                                        ::description
                                        ::documentation]
                               :opt-un [::infra-services]))

; Swarm

(s/def ::parent utils-spec/nonblank-string)
(s/def ::endpoint utils-spec/nonblank-string)
(s/def ::multiplicity int?)
(s/def ::cloud-vm-image utils-spec/nonblank-string)
(s/def ::cloud-vm-size utils-spec/nonblank-string)
(s/def ::cloud-vm-disk-size int?)
(s/def ::cloud-region utils-spec/nonblank-string)
(s/def ::cloud-security-group utils-spec/nonblank-string)
(s/def ::cloud-project utils-spec/nonblank-string)
(s/def ::coe-manager-install boolean?)
(s/def ::management-credential utils-spec/nonblank-string)

(s/def ::coe-service (s/keys :req-un [::name
                                      ::description
                                      ::management-credential]
                             :opt-un [::parent
                                      ::multiplicity
                                      ::cloud-vm-image
                                      ::cloud-vm-size
                                      ::cloud-vm-disk-size
                                      ::cloud-region
                                      ::cloud-project
                                      ::cloud-security-group]))

(s/def ::generic-service (s/keys :req-un [::name
                                          ::description
                                          ::endpoint]
                                 :opt-un [::parent]))


(s/def ::registry-service (s/keys :req-un [::name
                                           ::description
                                           ::endpoint]
                                  :opt-un [::parent]))


; SSH keys

(s/def ::ssh-keys-infra any?)

(s/def ::ssh-keys (s/nilable (s/coll-of string?)))


; MinIO

(s/def ::minio-service (s/keys :req-un [::name
                                        ::description
                                        ::endpoint]
                               :opt-un [::parent]))

(s/def ::management-credentials-available any?)

(s/def ::db (s/keys :req [::infra-service-groups
                          ::infra-services
                          ::page
                          ::management-credentials-available
                          ::elements-per-page]))

(def defaults
  {::infra-service-groups             nil
   ::infra-services                   {}
   ::service-modal-visible?           false
   ::add-service-modal-visible?       false
   ::is-new?                          false
   ::infra-service                    nil
   ::page                             1
   ::multiplicity                     1
   ::management-credentials-available nil
   ::elements-per-page                8})
