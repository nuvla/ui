(ns sixsq.nuvla.ui.deployment-fleets.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.edges.spec :as edges-spec]
    [sixsq.nuvla.ui.deployment-fleets.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-sub
  ::loading?
  (fn [db]
    (::edges-spec/loading? db)))


(reg-sub
  ::query-params
  (fn [db]
    (::edges-spec/query-params db)))


(reg-sub
  ::full-text-search
  (fn [db]
    (::edges-spec/full-text-search db)))


(reg-sub
  ::deployment-fleets
  (fn [db]
    (::spec/deployment-fleets db)))


(reg-sub
  ::nuvlabox-locations
  ::edges-spec/nuvlabox-locations)


(reg-sub
  ::deployment-fleets-summary
  (fn [db]
    (::spec/deployment-fleets-summary db)))


(reg-sub
  ::nuvlaboxes-summary-all
  (fn [db]
    (::edges-spec/nuvlaboxes-summary-all db)))


(reg-sub
  ::state-selector
  (fn [db]
    (::spec/state-selector db)))


(reg-sub
  ::elements-per-page
  (fn [db]
    (::spec/elements-per-page db)))


(reg-sub
  ::page
  (fn [db]
    (::spec/page db)))


(reg-sub
  ::state-nuvlaboxes
  (fn [db]
    (::edges-spec/state-nuvlaboxes db)))


(reg-sub
  ::modal-visible?
  (fn [db [_ modal-id]]
    (= modal-id (::edges-spec/open-modal db))))


(reg-sub
  ::nuvlabox-created-id
  (fn [db]
    (::edges-spec/nuvlabox-created-id db)))


(reg-sub
  ::nuvlabox-ssh-key
  (fn [db]
    (::edges-spec/nuvlabox-ssh-key db)))


(reg-sub
  ::nuvlabox-private-ssh-key
  (fn [db]
    (::edges-spec/nuvlabox-private-ssh-key db)))


(reg-sub
  ::nuvlabox-usb-api-key
  (fn [db]
    (::edges-spec/nuvlabox-usb-api-key db)))


(reg-sub
  ::vpn-infra-options
  (fn [{:keys [::edges-spec/vpn-infra]}]
    (map
      (fn [{:keys [id name]}] {:key id, :text name, :value id})
      ::edges-spec/vpn-infra)))


(reg-sub
  ::nuvlabox-releases
  (fn [db]
    (::edges-spec/nuvlabox-releases db)))


(reg-sub
  ::nuvlabox-releases-options
  :<- [::nuvlabox-releases]
  (fn [nuvlabox-releases]
    (map
      (fn [{:keys [id release]}]
        {:key release, :text release, :value id})
      nuvlabox-releases)))


(reg-sub
  ::ssh-keys-available
  (fn [db]
    (::edges-spec/ssh-keys-available db)))


(reg-sub
  ::nuvlabox-clusters
  (fn [db]
    (::edges-spec/nuvlabox-clusters db)))


(reg-sub
  ::nuvlabox-cluster
  (fn [db]
    (::edges-spec/nuvlabox-cluster db)))


(reg-sub
  ::can-edit-cluster?
  :<- [::nuvlabox-cluster]
  (fn [cluster _]
    (general-utils/can-edit? cluster)))


(reg-sub
  ::nuvlabox-not-found?
  (fn [db]
    (::edges-spec/nuvlabox-not-found? db)))


(reg-sub
  ::nuvlabox-playbooks-cronjob
  (fn [db]
    (::edges-spec/nuvlabox-playbooks-cronjob db)))


(reg-sub
  ::nuvlaboxes-in-clusters
  (fn [db]
    (::edges-spec/nuvlaboxes-in-clusters db)))
