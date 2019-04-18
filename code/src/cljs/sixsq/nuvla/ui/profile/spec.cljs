(ns sixsq.nuvla.ui.profile.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::credential-password string?)

(s/def ::open-modal (s/nilable #{:change-password}))

(s/def ::error-message (s/nilable string?))

(s/def ::form-data any?)


(s/def ::db (s/keys :req [::credential-password
                          ::open-modal
                          ::error-message
                          ::form-data]))


(def defaults {::credential-password nil
               ::open-modal          nil
               ::error-message       nil
               ::form-data           {}})

