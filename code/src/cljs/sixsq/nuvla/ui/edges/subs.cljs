(ns sixsq.nuvla.ui.edges.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.edges.spec :as spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.time :as time]))

(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))

(reg-sub
  ::query-params
  (fn [db]
    (::spec/query-params db)))

(reg-sub
  ::nuvlaboxes
  (fn [db]
    (::spec/nuvlaboxes db)))

(reg-sub
  ::edges-status
  (fn [db]
    (::spec/nuvlaedges-select-status db)))

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
  ::spec/nuvlabox-locations)

(reg-sub
  ::nuvlaboxes-summary
  (fn [db]
    (::spec/nuvlaboxes-summary db)))

(reg-sub
  ::nuvlaboxes-summary-all
  (fn [db]
    (::spec/nuvlaboxes-summary-all db)))

(reg-sub
  ::state-selector
  (fn [db]
    (::spec/state-selector db)))

(reg-sub
  ::state-nuvlaboxes
  (fn [db]
    (::spec/state-nuvlaboxes db)))

(reg-sub
  ::modal-visible?
  (fn [db [_ modal-id]]
    (= modal-id (::spec/open-modal db))))

(reg-sub
  ::nuvlabox-created-id
  (fn [db]
    (::spec/nuvlabox-created-id db)))

(reg-sub
  ::nuvlabox-ssh-key
  (fn [db]
    (::spec/nuvlabox-ssh-key db)))

(reg-sub
  ::nuvlabox-private-ssh-key
  (fn [db]
    (::spec/nuvlabox-private-ssh-key db)))

(reg-sub
  ::nuvlabox-usb-api-key
  (fn [db]
    (::spec/nuvlabox-usb-api-key db)))

(reg-sub
  ::vpn-infra-options
  (fn [{:keys [::spec/vpn-infra]}]
    (map
      (fn [{:keys [id name]}] {:key id, :text name, :value id})
      vpn-infra)))

(reg-sub
  ::nuvlabox-releases
  (fn [db]
    (::spec/nuvlabox-releases db)))

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
    (map
      (fn [{:keys [id release pre-release]}]
        {:key release, :text (str release (when pre-release " - pre-release")), :value id, :pre-release pre-release})
      nuvlabox-releases)))

(reg-sub
  ::ssh-keys-available
  (fn [db]
    (::spec/ssh-keys-available db)))

(reg-sub
  ::nuvlabox-clusters
  (fn [db]
    (::spec/nuvlabox-clusters db)))

(reg-sub
  ::nuvlabox-cluster
  (fn [db]
    (::spec/nuvlabox-cluster db)))

(reg-sub
  ::can-edit-cluster?
  :<- [::nuvlabox-cluster]
  (fn [cluster _]
    (general-utils/can-edit? cluster)))

(reg-sub
  ::nuvlabox-not-found?
  (fn [db]
    (::spec/nuvlabox-not-found? db)))

(reg-sub
  ::nuvlabox-playbooks-cronjob
  (fn [db]
    (::spec/nuvlabox-playbooks-cronjob db)))

(reg-sub
  ::nuvlaboxes-in-clusters
  (fn [db]
    (::spec/nuvlaboxes-in-clusters db)))

(reg-sub
  ::additional-filter
  (fn [db]
    (::spec/additional-filter db)))