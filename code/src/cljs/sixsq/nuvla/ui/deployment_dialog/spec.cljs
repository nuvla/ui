(ns sixsq.nuvla.ui.deployment-dialog.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.utils.icons :as icons]))


(s/def ::deploy-modal-visible? boolean?)

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

(s/def ::registries-creds any?)
(s/def ::invisible-registries-creds (s/coll-of any? :kind vector?))

(s/def ::data-clouds any?)
(s/def ::selected-cloud (s/nilable string?))
(s/def ::cloud-filter (s/nilable string?))

(s/def ::cloud-infra-services any?)

(s/def ::step-id #{:data :credentials :env-variables :files :summary})

(s/def ::active-step ::step-id)

(s/def ::icon string?)

(s/def ::status #{:loading :ok :warning})

(s/def ::step-state (s/keys :req-un [::step-id
                                     ::icon]
                            :opt-un [::status]))

(s/def ::data-step-active? boolean?)

(s/def ::step-states (s/map-of ::step-id ::step-state))

(s/def ::license-accepted? boolean?)

(s/def ::price-accepted? boolean?)

(s/def ::error-message any?)

(s/def ::check-dct any?)

(s/def ::module-info any?)

(s/def ::selected-version any?)

(s/def ::original-module any?)


(s/def ::submit-loading? boolean?)

(def step-states {:data           {:step-id :data
                                   :icon    "database"}
                  :infra-services {:step-id :infra-services
                                   :icon    "map marker alternate"}
                  :module-version {:step-id :module-version
                                   :icon    "list ol"}
                  :registries     {:step-id :registries
                                   :icon    icons/i-docker}
                  :env-variables  {:step-id :env-variables
                                   :icon    "list alternate outline"}
                  :files          {:step-id :files
                                   :icon    "file alternate outline"}
                  :license        {:step-id    :license
                                   :step-title :eula
                                   :icon       "book"}
                  :pricing        {:step-id :pricing
                                   :icon    "euro"}
                  :summary        {:step-id :summary
                                   :icon    "info"}})


(def defaults {::deploy-modal-visible?      false
               ::deployment                 nil
               ::credentials                nil
               ::credentials-loading?       false
               ::infra-services             nil
               ::infra-services-loading?    false
               ::infra-registries           nil
               ::infra-registries-loading?  false
               ::infra-registries-creds     nil
               ::registries-creds           {}
               ::invisible-registries-creds []
               ::selected-infra-service     nil
               ::selected-credential-id     nil
               ::data-clouds                nil
               ::selected-cloud             nil
               ::cloud-filter               nil
               ::cloud-infra-services       nil
               ::active-step                :data
               ::data-step-active           true
               ::license-accepted?          false
               ::price-accepted?            false
               ::error-message              nil
               ::step-states                step-states
               ::check-dct                  nil
               ::module-info                nil
               ::selected-version           nil
               ::original-module            nil
               ::submit-loading?            false})
