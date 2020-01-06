(ns sixsq.nuvla.ui.profile.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as us]))


(s/def ::credential-password string?)

(s/def ::open-modal (s/nilable keyword?))

(s/def ::error-message (s/nilable string?))

(s/def ::db (s/keys :req [::credential-password
                          ::open-modal
                          ::error-message]))


(def defaults {::credential-password nil
               ::open-modal          nil
               ::error-message       nil})
