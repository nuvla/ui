(ns sixsq.nuvla.ui.edges-detail.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.nav-tab :as tab-plugin]))

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


(def defaults {::nuvlabox                     nil
               ::nuvlabox-status              nil
               ::nuvlabox-associated-ssh-keys nil
               ::nuvlabox-peripherals         nil
               ::vuln-severity-selector       nil
               ::matching-vulns-from-db       nil
               ::nuvlabox-vulns               nil
               ::nuvlabox-managers            nil
               ::join-token                   nil
               ::nuvlabox-cluster             nil
               ::nuvlabox-not-found?          false
               ::nuvlabox-playbooks           nil
               ::infra-services               []
               ::nuvlabox-emergency-playbooks nil
               ::nuvlabox-current-playbook    nil
               ::events                       (events-plugin/build-spec
                                                :default-items-per-page 15)
               ::tab                          (tab-plugin/build-spec)
               ::nuvlaedge-release            nil})

(s/def ::deployment-pagination any?)

(def deployments-pagination {::deployment-pagination (pagination-plugin/build-spec
                                                       :default-items-per-page 25)})
