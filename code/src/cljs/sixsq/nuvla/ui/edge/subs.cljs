(ns sixsq.nuvla.ui.edge.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.edge.spec :as spec]
    [sixsq.nuvla.ui.edge.utils :as utils]))


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
  ::nuvlaboxes
  (fn [db]
    (::spec/nuvlaboxes db)))


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
  ::nuvlaboxes-online-status
  (fn [db]
    (::spec/nuvlaboxes-online-status db)))


(reg-sub
  ::nuvlabox-online-status
  :<- [::nuvlaboxes-online-status]
  (fn [resources [_ nuvlabox-id]]
    (let [online (-> resources
                     (get nuvlabox-id {})
                     :online)]
      (utils/status->keyword online))))


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
  ::ssh-keys-available
  (fn [db]
    (::spec/ssh-keys-available db)))
