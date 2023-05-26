(ns sixsq.nuvla.ui.apps-application.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
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

(s/def ::tab any?)

; create an initial entry for new application

(def defaults {::module-application               {::docker-compose       nil
                                                   ::requires-user-rights false
                                                   ::compatibility        "docker-compose"}
               ::license-validation-errors        #{}
               ::docker-compose-validation-errors #{}
               ::configuration-validation-errors   #{}
               ::tab (nav-tab/build-spec :default-tab :overview)})

(s/def ::deployment-pagination any?)

(def deployments-pagination {::deployment-pagination (pagination-plugin/build-spec
                                                       :default-items-per-page 25)})
