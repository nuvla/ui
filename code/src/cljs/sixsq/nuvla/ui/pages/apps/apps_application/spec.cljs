(ns sixsq.nuvla.ui.pages.apps.apps-application.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]
            [sixsq.nuvla.ui.utils.time :as time]))


; Validation

(s/def ::license-validation-errors set?)
(s/def ::docker-compose-validation-errors set?)
(s/def ::configuration-validation-errors set?)

(s/def ::loading? boolean?)
(s/def ::app-data any?)
(s/def ::timespan any?)


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
               ::loading?                         false
               ::app-data                         nil
               ::license-validation-errors        #{}
               ::docker-compose-validation-errors #{}
               ::configuration-validation-errors   #{}
               ::tab                              (nav-tab/build-spec :default-tab :overview)
               ::timespan                         {:timespan-option "last 15 minutes"
                                                   :from            (time/subtract-minutes (time/now) 15)
                                                   :to              (time/now)}})

(s/def ::deployment-pagination any?)

(def deployments-pagination {::deployment-pagination (pagination-plugin/build-spec
                                                       :default-items-per-page 25)})
