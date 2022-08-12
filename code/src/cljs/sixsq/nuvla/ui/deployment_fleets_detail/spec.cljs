(ns sixsq.nuvla.ui.deployment-fleets-detail.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::deployment-fleet (s/nilable any?))
(s/def ::nuvlabox-status (s/nilable any?))
(s/def ::nuvlabox-associated-ssh-keys (s/nilable any?))
(s/def ::nuvlabox-peripherals (s/nilable any?))
(s/def ::nuvlabox-events (s/nilable any?))
(s/def ::vuln-severity-selector (s/nilable any?))
(s/def ::matching-vulns-from-db (s/nilable any?))
(s/def ::elements-per-page int?)
(s/def ::page int?)
(s/def ::nuvlabox-vulns (s/nilable any?))
(s/def ::active-tab keyword?)
(s/def ::nuvlabox-managers (s/nilable any?))
(s/def ::join-token (s/nilable any?))
(s/def ::nuvlabox-cluster (s/nilable any?))
(s/def ::deployment-fleet-not-found? boolean?)
(s/def ::nuvlabox-playbooks (s/nilable any?))
(s/def ::infra-services (s/nilable coll?))
(s/def ::nuvlabox-emergency-playbooks (s/nilable any?))
(s/def ::nuvlabox-current-playbook (s/nilable any?))


(s/def ::db (s/keys :req [::deployment-fleet
                          ::nuvlabox-status
                          ::nuvlabox-associated-ssh-keys
                          ::nuvlabox-peripherals
                          ::nuvlabox-events
                          ::vuln-severity-selector
                          ::matching-vulns-from-db
                          ::elements-per-page
                          ::page
                          ::nuvlabox-vulns
                          ::active-tab
                          ::nuvlabox-managers
                          ::join-token
                          ::nuvlabox-cluster
                          ::deployment-fleet-not-found?
                          ::nuvlabox-playbooks
                          ::infra-services
                          ::nuvlabox-emergency-playbooks
                          ::nuvlabox-current-playbook]))

(def defaults {::deployment-fleet             nil
               ::nuvlabox-status              nil
               ::nuvlabox-associated-ssh-keys nil
               ::nuvlabox-peripherals         nil
               ::nuvlabox-events              nil
               ::vuln-severity-selector       nil
               ::matching-vulns-from-db       nil
               ::elements-per-page            15
               ::page                         1
               ::nuvlabox-vulns               nil
               ::active-tab                   :overview
               ::nuvlabox-managers            nil
               ::join-token                   nil
               ::nuvlabox-cluster             nil
               ::deployment-fleet-not-found?  false
               ::nuvlabox-playbooks           nil
               ::infra-services               []
               ::nuvlabox-emergency-playbooks nil
               ::nuvlabox-current-playbook    nil})
