(ns sixsq.nuvla.ui.apps-applications-sets.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]))


; Validation
(s/def ::configuration-validation-errors set?)

; Module

(s/def ::apps-set-name spec-utils/nonblank-string)
(s/def ::apps-set-description (s/nilable string?))

(s/def ::apps-validation-errors set?)

(s/def ::apps-set-application-id string?)
(s/def ::apps-set-application-version nat-int?)

(s/def ::apps-application (s/keys :req [::apps-set-application-id
                                        ::apps-set-application-version]))
(s/def ::apps-applications (s/map-of any? ::apps-application))

(s/def ::apps-set (s/keys :req [::apps-set-name
                                ::apps-applications]
                          :opt [::apps-set-description]))

(s/def ::apps-sets (s/map-of any? ::apps-set))

; create an initial entry for new application

(def defaults {::apps-sets                       {0 {::apps-set-name ""}}
               ::configuration-validation-errors #{}
               ::apps-validation-errors          #{}})
