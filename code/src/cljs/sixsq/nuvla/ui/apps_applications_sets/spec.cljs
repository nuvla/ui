(ns sixsq.nuvla.ui.apps-applications-sets.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.module-selector :as module-selector]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]))


(def app-set-docker-subtype "docker")
(def app-set-k8s-subtype "kubernetes")

; Validation
(s/def ::configuration-validation-errors set?)

; Module

(s/def ::apps-set-name spec-utils/nonblank-string)
(s/def ::apps-set-description (s/nilable string?))

(s/def ::apps-validation-errors set?)

(s/def ::id string?)
(s/def ::version nat-int?)

(s/def ::apps-set-subtype #{app-set-docker-subtype
                            app-set-k8s-subtype})

(s/def ::apps-application (s/keys :req-un [::id]
                                  :opt-un [::version]))
(s/def ::apps-selected (s/map-of string? ::apps-application))

(s/def ::apps-set (s/keys :req [::apps-set-name]
                          :opt [::apps-selected
                                ::apps-set-description
                                ::apps-set-subtype]))

(s/def ::apps-sets (s/map-of any? ::apps-set))

; create an initial entry for new application

(def defaults {::apps-sets                       {1 {:id             1
                                                     ::apps-set-name ""
                                                     ::apps-selector (module-selector/build-spec)}}
               ::configuration-validation-errors #{}
               ::apps-validation-errors          #{}})
