(ns sixsq.nuvla.ui.edges.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [build-ordering] :as table-plugin]))

(def resource-name "nuvlabox")

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
(s/def ::edges-tags (s/nilable (s/* string?)))
(s/def ::select (s/nilable any?))
(s/def ::edges-without-edit-rights any?)

; ssh key association
(s/def ::ssh-keys-available any?)
(s/def ::nuvlabox-clusters any?)
(s/def ::nuvlabox-cluster any?)
(s/def ::nuvlaboxes-in-clusters any?)
(s/def ::nuvlabox-not-found? boolean?)
(s/def ::nuvlabox-playbooks-cronjob any?)

(s/def ::additional-filter (s/nilable string?))

(def columns
  [:online :state :name :description :created
   :created-by :refresh-interval :last-online :version :tags :manager])

(s/def ::ordering
  (s/cat :field (set columns) :order #{"desc" "asc"}))

(def default-ordering {:field :created :order "desc"})

(def local-storage-key "nuvla.ui.edges-preferences")

(def cards-view :cards)
(def table-view :table)
(def map-view :map)
(def cluster-view :cluster)

(def view-types [cards-view table-view map-view cluster-view])


(def modal-add-id ::add)
(def modal-tags-set-id ::tags-set)
(def modal-tags-add-id ::tags-add)
(def modal-tags-remove-id ::tags-remove)
(def modal-tags-remove-all ::tags-remove-all)

(def tags-modal-ids [modal-tags-add-id modal-tags-set-id modal-tags-remove-id modal-tags-remove-all])


(def defaults
  {::nuvlaboxes                    nil
   ::next-heartbeats-offline-edges nil
   ::nuvlabox-cluster-summary      nil
   ::nuvlaboxes-summary            nil
   ::nuvlaboxes-summary-all        nil
   ::nuvlabox-releases             nil
   ::state-nuvlaboxes              nil
   ::state-selector                nil
   ::open-modal                    nil
   ::nuvlabox-created-id           nil
   ::nuvlabox-usb-api-key          nil
   ::nuvlabox-ssh-key              nil
   ::nuvlabox-private-ssh-key      nil
   ::vpn-infra                     nil
   ::ssh-keys-available            nil
   ::nuvlabox-clusters             nil
   ::nuvlabox-cluster              nil
   ::nuvlaboxes-in-clusters        nil
   ::nuvlabox-playbooks-cronjob    nil
   ::ordering                      (build-ordering)
   ::edges-search                  (full-text-search-plugin/build-spec)
   ::additional-filter             nil
   ::select                        (table-plugin/build-bulk-edit-spec)
   })

(def pagination-default {::pagination (pagination-plugin/build-spec
                                        :default-items-per-page 25)})
