(ns sixsq.nuvla.ui.edge-detail.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)
(s/def ::nuvlabox (s/nilable string?))
(s/def ::nuvlabox-status (s/nilable any?))
(s/def ::nuvlabox-ssh-keys (s/nilable any?))
(s/def ::nuvlabox-peripherals (s/nilable any?))


(s/def ::db (s/keys :req [::loading?
                          ::nuvlabox
                          ::nuvlabox-status
                          ::nuvlabox-ssh-keys
                          ::nuvlabox-peripherals]))


(def defaults {::loading?             true
               ::nuvlabox             nil
               ::nuvlabox-status      nil
               ::nuvlabox-ssh-keys    nil
               ::nuvlabox-peripherals nil})
