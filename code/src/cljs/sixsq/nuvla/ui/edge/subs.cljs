(ns sixsq.nuvla.ui.edge.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.edge.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::query-params
  (fn [db]
    (::spec/query-params db)))


(reg-sub
  ::full-text-search
  (fn [db]
    (::spec/full-text-search db)))


(reg-sub
  ::full-text-clusters-search
  (fn [db]
    (::spec/full-text-clusters-search db)))


(reg-sub
  ::nuvlaboxes
  (fn [db]
    (::spec/nuvlaboxes db)))


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
  ::active-tab-index
  (fn [db]
    (get-in db [::spec/active-tab-index])))


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
