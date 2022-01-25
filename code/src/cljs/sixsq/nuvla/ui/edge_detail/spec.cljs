(ns sixsq.nuvla.ui.edge-detail.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.time :as time]))


(s/def ::nuvlabox (s/nilable any?))
(s/def ::nuvlabox-status (s/nilable any?))
(s/def ::nuvlabox-associated-ssh-keys (s/nilable any?))
(s/def ::nuvlabox-peripherals (s/nilable any?))
(s/def ::nuvlabox-events (s/nilable any?))
(s/def ::vuln-severity-selector (s/nilable any?))
(s/def ::matching-vulns-from-db (s/nilable any?))
(s/def ::elements-per-page int?)
(s/def ::page int?)
(s/def ::nuvlabox-vulns (s/nilable any?))
(s/def ::active-tab-index number?)
(s/def ::nuvlabox-managers (s/nilable any?))
(s/def ::join-token (s/nilable any?))
(s/def ::nuvlabox-cluster (s/nilable any?))
(s/def ::nuvlabox-not-found? boolean?)
(s/def ::nuvlabox-playbooks (s/nilable any?))
(s/def ::nuvlabox-emergency-playbooks (s/nilable any?))
(s/def ::nuvlabox-current-playbook (s/nilable any?))
(s/def ::nuvlabox-log any?)
(s/def ::nuvlabox-log-id (s/nilable string?))
(s/def ::nuvlabox-log-since (s/nilable string?))
(s/def ::nuvlabox-log-play? boolean?)
(s/def ::nuvlabox-log-components any?)


(s/def ::db (s/keys :req [::nuvlabox
                          ::nuvlabox-status
                          ::nuvlabox-associated-ssh-keys
                          ::nuvlabox-peripherals
                          ::nuvlabox-events
                          ::vuln-severity-selector
                          ::matching-vulns-from-db
                          ::elements-per-page
                          ::page
                          ::nuvlabox-vulns
                          ::active-tab-index
                          ::nuvlabox-managers
                          ::join-token
                          ::nuvlabox-cluster
                          ::nuvlabox-not-found?
                          ::nuvlabox-playbooks
                          ::nuvlabox-emergency-playbooks
                          ::nuvlabox-current-playbook
                          ::nuvlabox-log
                          ::nuvlabox-log-id
                          ::nuvlabox-log-since
                          ::nuvlabox-log-play?
                          ::nuvlabox-log-components]))

(defn default-since []
  (-> (time/now) (.seconds 0)))

(def defaults {::nuvlabox                     nil
               ::nuvlabox-status              nil
               ::nuvlabox-associated-ssh-keys nil
               ::nuvlabox-peripherals         nil
               ::nuvlabox-events              nil
               ::vuln-severity-selector       nil
               ::matching-vulns-from-db       nil
               ::elements-per-page            15
               ::page                         1
               ::nuvlabox-vulns               nil
               ::active-tab-index             0
               ::nuvlabox-managers            nil
               ::join-token                   nil
               ::nuvlabox-cluster             nil
               ::nuvlabox-not-found?          false
               ::nuvlabox-playbooks           nil
               ::nuvlabox-emergency-playbooks nil
               ::nuvlabox-current-playbook    nil
               ::nuvlabox-log                 nil
               ::nuvlabox-log-id              nil
               ::nuvlabox-log-play?           false
               ::nuvlabox-log-since           (default-since)
               ::nuvlabox-log-components      nil})
