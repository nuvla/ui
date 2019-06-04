(ns sixsq.nuvla.ui.profile.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as us]))


(s/def ::credential-password string?)

(s/def ::open-modal (s/nilable #{:change-password}))

(s/def ::form-data any?)

(s/def ::error-message (s/nilable string?))

; General

(s/def ::name us/nonblank-string)
(s/def ::description us/nonblank-string)

(s/def ::db (s/keys :req [::credential-password
                          ::open-modal
                          ::form-data
                          ::error-message]))


(def defaults {::credential-password nil
               ::open-modal          nil
               ::form-data           {}
               ::error-message       nil})


;; Validation spec

(s/def ::current-password us/nonblank-string)

(s/def ::new-password ::us/password)

(s/def ::repeat-new-password ::us/password)

(s/def ::change-password-form (s/keys :req-un [::current-password
                                               ::new-password
                                               ::repeat-new-password]))

