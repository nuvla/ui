(ns sixsq.nuvla.ui.pages.edges-detail.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.common-components.plugins.audit-log :as audit-log-plugin]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as tab-plugin]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]
            [sixsq.nuvla.ui.utils.time :as time]))

(def stats-table-col-configs-local-storage-key "nuvla.ui.table.edges.stats.column-configs")

(s/def ::nuvlabox (s/nilable any?))
(s/def ::nuvlabox-status (s/nilable any?))
(s/def ::nuvlabox-associated-ssh-keys (s/nilable any?))
(s/def ::nuvlabox-peripherals (s/nilable any?))
(s/def ::vuln-severity-selector (s/nilable any?))
(s/def ::matching-vulns-from-db (s/nilable any?))
(s/def ::nuvlabox-vulns (s/nilable any?))
(s/def ::tab any?)
(s/def ::nuvlabox-managers (s/nilable any?))
(s/def ::join-token (s/nilable any?))
(s/def ::nuvlabox-cluster (s/nilable any?))
(s/def ::nuvlabox-not-found? boolean?)
(s/def ::nuvlabox-playbooks (s/nilable any?))
(s/def ::infra-services (s/nilable coll?))
(s/def ::nuvlabox-emergency-playbooks (s/nilable any?))
(s/def ::nuvlabox-current-playbook (s/nilable any?))


(s/def ::id string?)
(s/def ::pre-release boolean?)
(s/def ::nuvlaedge-release (s/nilable (s/keys :req-un [::id
                                                       ::pre-release])))

(s/def ::edge-stats (s/nilable any?))

(s/def ::timespan (s/nilable any?))
(s/def ::availability-15-min nil)

(s/def ::registry-id spec-utils/nonblank-string)
(s/def ::registry-cred-id string?)
(s/def ::single-registry (s/keys :req [::registry-cred-id
                                       ::registry-id]))

(s/def ::registries (s/map-of any? (s/merge ::single-registry)))

(def defaults {::coe-resource-docker-available? false
               ::coe-resource-k8s-available?    false
               ::nuvlabox                       nil
               ::nuvlabox-status                nil
               ::nuvlabox-status-set-time       nil
               ::nuvlabox-associated-ssh-keys   nil
               ::nuvlabox-peripherals           nil
               ::vuln-severity-selector         nil
               ::matching-vulns-from-db         nil
               ::nuvlabox-vulns                 nil
               ::nuvlabox-managers              nil
               ::join-token                     nil
               ::nuvlabox-cluster               nil
               ::nuvlabox-not-found?            false
               ::nuvlabox-playbooks             nil
               ::infra-services                 []
               ::nuvlabox-emergency-playbooks   nil
               ::nuvlabox-current-playbook      nil
               ::events                         (audit-log-plugin/build-spec
                                                  :default-items-per-page 15
                                                  :default-show-all-events? true)
               ::tab                            (tab-plugin/build-spec)
               ::nuvlaedge-release              nil
               ::edge-stats                     nil
               ::stats-loading?                 nil
               ::timespan                       {:timespan-option "last 15 minutes"
                                                 :from            (time/subtract-minutes (time/now) 15)
                                                 :to              (time/now)}
               ::availability-15-min            nil

               ::registry-id                    nil
               ::registry-cred-id               nil
               ::registries                     nil})

(s/def ::deployment-pagination any?)

(def deployments-pagination {::deployment-pagination (pagination-plugin/build-spec
                                                       :default-items-per-page 25)})
