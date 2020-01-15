(ns sixsq.nuvla.ui.deployment-dialog.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::deploy-modal-visible? boolean?)

(s/def ::loading-deployment? boolean?)
(s/def ::deployment any?)

(s/def ::credentials (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::credentials-loading? boolean?)
(s/def ::selected-credential any?)
(s/def ::coe-check-loading? boolean?)
(s/def ::coe-check-job-id (s/nilable string?))
(s/def ::coe-check any?)


(s/def ::infra-services (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::infra-services-loading? boolean?)
(s/def ::selected-infra-service any?)

(s/def ::data-clouds any?)
(s/def ::selected-cloud (s/nilable string?))
(s/def ::cloud-filter (s/nilable string?))

(s/def ::cloud-infra-services any?)

(s/def ::step-id #{:data :credentials :env-variables :files :summary})

(s/def ::active-step ::step-id)

(s/def ::loading? boolean?)

(s/def ::icon string?)

(s/def ::step-state (s/keys :req-un [::step-id
                                     ::icon]))

(s/def ::data-step-active? boolean?)

(s/def ::step-states (s/map-of ::step-id ::step-state))


(s/def ::db (s/keys :req [::deploy-modal-visible?
                          ::loading-deployment?
                          ::deployment
                          ::infra-services
                          ::infra-services-loading?
                          ::selected-infra-service
                          ::credentials
                          ::credentials-loading?
                          ::coe-check-loading?
                          ::coe-check
                          ::selected-credential
                          ::data-clouds
                          ::selected-cloud
                          ::cloud-filter
                          ::cloud-infra-services            ;; from search to display data

                          ::active-step
                          ::data-step-active?
                          ::step-states]))


(def defaults {::deploy-modal-visible?   false
               ::loading-deployment?     false
               ::deployment              nil
               ::credentials             nil
               ::credentials-loading?    false
               ::coe-check-loading?      false
               ::coe-check               nil
               ::infra-services          nil
               ::infra-services-loading? false
               ::selected-infra-service  nil
               ::selected-credential     nil
               ::data-clouds             nil
               ::selected-cloud          nil
               ::cloud-filter            nil
               ::cloud-infra-services    nil

               ::active-step             :data
               ::data-step-active        true
               ::step-states             {:data           {:step-id :data
                                                           :icon    "database"}
                                          :infra-services {:step-id :infra-services
                                                           :icon    "cloud"}
                                          :env-variables  {:step-id :env-variables
                                                           :icon    "list alternate outline"}
                                          :files          {:step-id :files
                                                           :icon    "file alternate outline"}
                                          :summary        {:step-id :summary
                                                           :icon    "info"}}})
