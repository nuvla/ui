(ns sixsq.nuvla.ui.edges.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
    [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
    [sixsq.nuvla.ui.plugins.table :refer [build-ordering]]))

(s/def ::stale-count nat-int?)
(s/def ::active-count nat-int?)
(s/def ::nuvlaboxes any?)
(s/def ::nuvlaedges-select-status map?)
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
(s/def ::edges-search any?)
(s/def ::state-selector #{"all" "new" "activated" "commissioned"
                          "decommissioning" "decommissioned" "error"})
(s/def ::vpn-infra any?)

; ssh key association
(s/def ::ssh-keys-available any?)
(s/def ::nuvlabox-clusters any?)
(s/def ::nuvlabox-cluster any?)
(s/def ::nuvlaboxes-in-clusters any?)
(s/def ::nuvlabox-not-found? boolean?)
(s/def ::nuvlabox-playbooks-cronjob any?)

(def columns
  [:online :state :name :description :created
   :created-by :refresh-interval :last-online :version :tags :manager])

(s/def ::ordering
  (s/cat :field (into #{} columns) :order #{"desc" "asc"}))

(def default-ordering {:field :created :order "desc"})

(def defaults
  {::nuvlaboxes                   nil
   ::next-heartbeats-offline-edges nil
   ::nuvlabox-cluster-summary     nil
   ::nuvlaboxes-summary           nil
   ::nuvlaboxes-summary-all       nil
   ::nuvlabox-releases            nil
   ::state-nuvlaboxes             nil
   ::state-selector               nil
   ::open-modal                   nil
   ::nuvlabox-created-id          nil
   ::nuvlabox-usb-api-key         nil
   ::nuvlabox-ssh-key             nil
   ::nuvlabox-private-ssh-key     nil
   ::vpn-infra                    nil
   ::ssh-keys-available           nil
   ::nuvlabox-clusters            nil
   ::nuvlabox-cluster             nil
   ::nuvlaboxes-in-clusters       nil
   ::nuvlabox-playbooks-cronjob   nil
   ::ordering                     (build-ordering)
   ::pagination                   (pagination-plugin/build-spec
                                    :default-items-per-page 25)
   ::edges-search                 (full-text-search-plugin/build-spec)})
