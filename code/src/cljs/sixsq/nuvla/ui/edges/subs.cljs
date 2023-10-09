(ns sixsq.nuvla.ui.edges.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [sixsq.nuvla.ui.edges.spec :as spec]
            [sixsq.nuvla.ui.edges.utils :as utils]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.time :as time]))

(reg-sub
  ::loading?
  :-> ::spec/loading?)

(reg-sub
  ::view-type
  :<- [::route-subs/query-param :view]
  (fn [view-type]
    (keyword (or view-type spec/table-view))))

(reg-sub
  ::nuvlaboxes
  :-> ::spec/nuvlaboxes)

(reg-sub
  ::nuvlaboxes-resources
  :<- [::nuvlaboxes]
  :-> :resources)

(reg-sub
  ::nuvlaboxes-count
  :<- [::nuvlaboxes]
  (fn [nuvlaboxes]
    (get nuvlaboxes :count 0)))

(reg-sub
  ::edges-without-edit-rights
  :-> ::spec/edges-without-edit-rights)


(reg-sub
  ::edges-tags
  :-> ::spec/edges-tags)


(reg-sub
  ::edges-status
  :-> ::spec/nuvlaedges-select-status)

(reg-sub
  ::next-heartbeat-moment
  :<- [::edges-status]
  (fn [status [_ edge-id]]
    (some-> (get-in status [edge-id :next-heartbeat]) time/parse-iso8601)))

(reg-sub
  ::engine-version
  :<- [::edges-status]
  (fn [edges-status [_ edge-id]]
    (get-in edges-status [edge-id :nuvlabox-engine-version])))

(reg-sub
  ::one-edge-with-only-major-version
  :<- [::edges-status]
  (fn [edges-status [_ ids]]
    (if edges-status
      (some (comp nil? :nuvlabox-engine-version edges-status) ids)
      false)))

(reg-sub
  ::nuvlabox-locations
  :-> ::spec/nuvlabox-locations)

(reg-sub
  ::nuvlaboxes-summary
  :-> ::spec/nuvlaboxes-summary)


(reg-sub
  ::nuvlaboxes-summary-stats
  :<- [::nuvlaboxes-summary]
  (fn [summary]
    (utils/summary-stats summary)))

(reg-sub
  ::nuvlaboxes-summary-all
  :-> ::spec/nuvlaboxes-summary-all)

(reg-sub
  ::nuvlaboxes-summary-all-stats
  :<- [::nuvlaboxes-summary-all]
  (fn [summary]
    (utils/summary-stats summary)))

(reg-sub
  ::state-selector
  :-> ::spec/state-selector)

(reg-sub
  ::state-nuvlaboxes
  :-> ::spec/state-nuvlaboxes)

(reg-sub
  ::opened-modal
  :-> ::spec/open-modal)

(reg-sub
  ::modal-visible?
  :<- [::opened-modal]
  (fn [opened-modal [_ modal-id]]
    (= modal-id opened-modal)))

(reg-sub
  ::bulk-modal-visible?
  :<- [::opened-modal]
  (fn [opened-modal]
    (boolean ((set spec/tags-modal-ids) opened-modal))))

(reg-sub
  ::nuvlabox-created-id
  :-> ::spec/nuvlabox-created-id)

(reg-sub
  ::nuvlabox-ssh-key
  :-> ::spec/nuvlabox-ssh-key)

(reg-sub
  ::nuvlabox-private-ssh-key
  :-> ::spec/nuvlabox-private-ssh-key)

(reg-sub
  ::nuvlabox-usb-api-key
  :-> ::spec/nuvlabox-usb-api-key)

(reg-sub
  ::vpn-infra-options
  (fn [{:keys [::spec/vpn-infra]}]
    (map
      (fn [{:keys [id name]}] {:key id, :text name, :value id})
      vpn-infra)))

(reg-sub
  ::nuvlabox-releases-response
  :-> ::spec/nuvlabox-releases)

(reg-sub
  ::nuvlabox-releases
  :<- [::nuvlabox-releases-response]
  (fn [nuvlabox-releases]
    (utils/sort-by-version nuvlabox-releases)))


(reg-sub
  ::nuvlabox-releases-by-id
  :<- [::nuvlabox-releases]
  (fn [nuvlabox-releases]
    (zipmap (map :id nuvlabox-releases) nuvlabox-releases)))

(reg-sub
  ::nuvlabox-releases-from-id
  :<- [::nuvlabox-releases-by-id]
  (fn [nuvlabox-releases-by-id [_ id]]
    (nuvlabox-releases-by-id id)))

(reg-sub
  ::nuvlabox-releases-by-release-number
  :<- [::nuvlabox-releases]
  (fn [nuvlabox-releases]
    (zipmap (map :release nuvlabox-releases) nuvlabox-releases)))


(reg-sub
  ::nuvlabox-releases-options
  :<- [::nuvlabox-releases]
  (fn [nuvlabox-releases]
    (->> (utils/sort-by-version nuvlabox-releases)
         (map
           (fn [{:keys [id release pre-release]}]
             {:key release, :text (str release (when pre-release " - pre-release")),
              :value id, :pre-release pre-release})))))

(reg-sub
  ::ssh-keys-available
  :-> ::spec/ssh-keys-available)

(reg-sub
  ::nuvlabox-clusters
  :-> ::spec/nuvlabox-clusters)

(reg-sub
  ::nuvlabox-cluster
  :-> ::spec/nuvlabox-cluster)

(reg-sub
  ::can-edit-cluster?
  :<- [::nuvlabox-cluster]
  (fn [cluster _]
    (general-utils/can-edit? cluster)))

(reg-sub
  ::nuvlabox-not-found?
  :-> ::spec/nuvlabox-not-found?)

(reg-sub
  ::nuvlabox-playbooks-cronjob
  :-> ::spec/nuvlabox-playbooks-cronjob)

(reg-sub
  ::nuvlaboxes-in-clusters
  :-> ::spec/nuvlaboxes-in-clusters)

(reg-sub
  ::additional-filter
  :-> ::spec/additional-filter)

(reg-sub
  ::selection
  :-> ::spec/select)