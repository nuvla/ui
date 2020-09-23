(ns sixsq.nuvla.ui.deployment-dialog.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::deploy-modal-visible? boolean?)

(s/def ::loading-deployment? boolean?)
(s/def ::deployment any?)

(s/def ::credentials any?)
(s/def ::credentials-loading? boolean?)
(s/def ::selected-credential-id any?)


(s/def ::infra-services (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::infra-services-loading? boolean?)
(s/def ::selected-infra-service any?)

(s/def ::infra-registries (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::infra-registries-loading? boolean?)

(s/def ::infra-registries-creds (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::infra-registries-creds-loading? boolean?)

(s/def ::registries-creds any?)

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

(s/def ::license-accepted? boolean?)

(s/def ::price-accepted? boolean?)


(s/def ::db (s/keys :req [::deploy-modal-visible?
                          ::loading-deployment?
                          ::deployment
                          ::infra-services
                          ::infra-services-loading?
                          ::infra-registries
                          ::infra-registries-loading?
                          ::infra-registries-creds
                          ::infra-registries-creds-loading?
                          ::registries-creds
                          ::selected-infra-service
                          ::credentials
                          ::credentials-loading?
                          ::selected-credential-id
                          ::data-clouds
                          ::selected-cloud
                          ::cloud-filter
                          ::cloud-infra-services            ;; from search to display data
                          ::license-accepted?
                          ::price-accepted?

                          ::active-step
                          ::data-step-active?
                          ::step-states]))


(def step-states {:data           {:step-id :data
                                   :icon    "database"}
                  :infra-services {:step-id :infra-services
                                   :icon    "cloud"}
                  :registries     {:step-id :registries
                                   :icon    "docker"}
                  :env-variables  {:step-id :env-variables
                                   :icon    "list alternate outline"}
                  :files          {:step-id :files
                                   :icon    "file alternate outline"}
                  :license        {:step-id :license
                                   :icon    "book"}
                  :billing        {:step-id :billing
                                   :icon    "euro"}
                  :summary        {:step-id :summary
                                   :icon    "info"}})


(def defaults {::deploy-modal-visible?           false
               ::loading-deployment?             false
               ::deployment                      nil
               ::credentials                     nil
               ::credentials-loading?            false
               ::infra-services                  nil
               ::infra-services-loading?         false
               ::infra-registries                nil
               ::infra-registries-loading?       false
               ::infra-registries-creds          nil
               ::infra-registries-creds-loading? false
               ::registries-creds                {}
               ::selected-infra-service          nil
               ::selected-credential-id          nil
               ::data-clouds                     nil
               ::selected-cloud                  nil
               ::cloud-filter                    nil
               ::cloud-infra-services            nil
               ::active-step                     :data
               ::data-step-active                true
               :license-accepted?                false
               ::price-accepted?                 false
               ::step-states                     step-states})
