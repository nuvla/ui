(ns sixsq.nuvla.ui.edge.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::stale-count nat-int?)

(s/def ::active-count nat-int?)

(s/def ::nuvlaboxes any?)

(s/def ::nuvlabox-locations any?)

(s/def ::nuvlabox-cluster-summary any?)

(s/def ::nuvlaboxes-summary any?)

(s/def ::nuvlaboxes-summary-all any?)

(s/def ::nuvlabox-releases any?)

(s/def ::state-nuvlaboxes any?)

(s/def ::open-modal (s/nilable keyword?))

(s/def ::nuvlabox-created-id (s/nilable string?))

(s/def ::nuvlabox-usb-api-key any?)

(s/def ::nuvlabox-ssh-key any?)
(s/def ::nuvlabox-private-ssh-key string?)

(s/def ::page int?)
(s/def ::elements-per-page int?)
(s/def ::total-elements int?)

(s/def ::active-tab-index number?)

(s/def ::full-text-search (s/nilable string?))
(s/def ::full-text-clusters-search (s/nilable string?))

(s/def ::state-selector #{"all" "new" "activated" "commissioned"
                          "decommissioning" "decommissioned" "error"})

(s/def ::vpn-infra any?)

; ssh key association
(s/def ::ssh-keys-available any?)

(s/def ::nuvlabox-clusters any?)

(s/def ::nuvlabox-cluster any?)


(s/def ::nuvlabox-not-found? boolean?)


(s/def ::db (s/keys :req [::nuvlaboxes
                          ::nuvlaboxes-summary
                          ::nuvlabox-cluster-summary
                          ::nuvlaboxes-summary-all
                          ::nuvlabox-releases
                          ::state-nuvlaboxes
                          ::page
                          ::elements-per-page
                          ::total-elements
                          ::active-tab-index
                          ::full-text-search
                          ::full-text-clusters-search
                          ::state-selector
                          ::open-modal
                          ::nuvlabox-created-id
                          ::nuvlabox-usb-api-key
                          ::nuvlabox-ssh-key
                          ::nuvlabox-private-ssh-key
                          ::vpn-infra
                          ::ssh-keys-available
                          ::nuvlabox-clusters
                          ::nuvlabox-cluster]))


(def defaults {::nuvlaboxes                nil
               ::nuvlabox-cluster-summary  nil
               ::nuvlaboxes-summary        nil
               ::nuvlaboxes-summary-all    nil
               ::nuvlabox-releases         nil
               ::state-nuvlaboxes          nil
               ::page                      1
               ::elements-per-page         8
               ::total-elements            0
               ::active-tab-index          0
               ::full-text-search          nil
               ::full-text-clusters-search nil
               ::state-selector            nil
               ::open-modal                nil
               ::nuvlabox-created-id       nil
               ::nuvlabox-usb-api-key      nil
               ::nuvlabox-ssh-key          nil
               ::nuvlabox-private-ssh-key  nil
               ::vpn-infra                 nil
               ::ssh-keys-available        nil
               ::nuvlabox-clusters         nil
               ::nuvlabox-cluster          nil})
