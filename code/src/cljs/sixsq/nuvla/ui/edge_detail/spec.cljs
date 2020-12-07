(ns sixsq.nuvla.ui.edge-detail.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)
(s/def ::nuvlabox (s/nilable string?))
(s/def ::nuvlabox-status (s/nilable any?))
(s/def ::nuvlabox-ssh-keys (s/nilable any?))
(s/def ::nuvlabox-peripherals (s/nilable any?))
(s/def ::nuvlabox-events (s/nilable any?))
(s/def ::vuln-severity-selector (s/nilable any?))
(s/def ::elements-per-page int?)
(s/def ::page int?)
(s/def ::nuvlabox-vulns (s/nilable any?))


(s/def ::db (s/keys :req [::loading?
                          ::nuvlabox
                          ::nuvlabox-status
                          ::nuvlabox-ssh-keys
                          ::nuvlabox-peripherals
                          ::nuvlabox-events
                          ::vuln-severity-selector
                          ::elements-per-page
                          ::page
                          ::nuvlabox-vulns]))


(def defaults {::loading?             true
               ::nuvlabox             nil
               ::nuvlabox-status      nil
               ::nuvlabox-ssh-keys    nil
               ::nuvlabox-peripherals nil
               ::nuvlabox-events      nil
               ::vuln-severity-selector nil
               ::elements-per-page    15
               ::page                 1
               ::nuvlabox-vulns       nil})
