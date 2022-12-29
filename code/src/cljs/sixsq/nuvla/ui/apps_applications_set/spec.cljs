(ns sixsq.nuvla.ui.apps-applications-set.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]))


; Validation

(s/def ::license-validation-errors set?)
(s/def ::docker-compose-validation-errors set?)
(s/def ::configuration-validation-errors set?)


(s/def ::requires-user-rights boolean?)


; Docker-compose

(s/def ::docker-compose (s/and spec-utils/nonblank-string
                               #(first (general-utils/check-yaml %))))


; Files

(s/def ::file-content string?)


(s/def ::file-name ::spec-utils/filename)


(s/def ::file (s/keys :req [::file-name ::file-content]))


(s/def ::files (s/map-of any? (s/merge ::file)))


; Module

(s/def ::module-application (s/keys :req [::docker-compose]
                                    :opt [::files
                                          ::requires-user-rights]))


(s/def ::apps-group-name string?)

(s/def ::apps-group (s/keys :req [::apps-group-name]))

(s/def ::apps-groups (s/map-of any? (s/merge ::apps-group)))

; create an initial entry for new application

(def defaults {::apps-groups                      {0 {::apps-group-name ""}}
               ::module-application               {::docker-compose       nil
                                                   ::requires-user-rights false}
               ::license-validation-errors        #{}
               ::docker-compose-validation-errors #{}
               ::configuration-validation-errors  #{}})
