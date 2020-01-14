(ns sixsq.nuvla.ui.deployment-dialog.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::deploy-modal-visible? boolean?)

(s/def ::loading-deployment? boolean?)
(s/def ::deployment any?)

(s/def ::loading-credentials? boolean?)
(s/def ::credentials (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::selected-credential any?)

(s/def ::data-infra-services any?)
(s/def ::selected-infra-service (s/nilable string?))
(s/def ::infra-service-filter (s/nilable string?))

(s/def ::infra-services any?)

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
                          ::loading-credentials?
                          ::credentials
                          ::selected-credential
                          ::data-infra-services
                          ::selected-infra-service
                          ::infra-service-filter
                          ::infra-services

                          ::active-step
                          ::data-step-active?
                          ::step-states]))


(def defaults {::deploy-modal-visible?  false
               ::loading-deployment?    false
               ::deployment             nil
               ::loading-credentials?   false
               ::credentials            nil
               ::selected-credential    nil
               ::data-infra-services    nil
               ::selected-infra-service nil
               ::infra-service-filter   nil
               ::infra-services         nil

               ::active-step            :data
               ::data-step-active       true
               ::step-states            {:data          {:step-id :data
                                                         :icon    "database"}
                                         :credentials   {:step-id :credentials
                                                         :icon    "key"}
                                         :env-variables {:step-id :env-variables
                                                         :icon    "list alternate outline"}
                                         :files         {:step-id :files
                                                         :icon    "file alternate outline"}
                                         :summary       {:step-id :summary
                                                         :icon    "info"}}})
