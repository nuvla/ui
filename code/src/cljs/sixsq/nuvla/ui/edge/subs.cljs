(ns sixsq.nuvla.ui.edge.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.edge.spec :as spec]))


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
  ::status-nuvlaboxes
  (fn [db]
    (::spec/status-nuvlaboxes db)))


(reg-sub
  ::status-nuvlabox
  :<- [::status-nuvlaboxes]
  (fn [{:keys [online offline]} [_ nuvlabox-id]]
    (cond
      (contains? online nuvlabox-id) :online
      (contains? offline nuvlabox-id) :offline
      :else :unknown)))

(reg-sub
  ::modal-visible?
  (fn [db [_ modal-id]]
    (= modal-id (::spec/open-modal db))))

(reg-sub
  ::nuvlabox-created-id
  (fn [db]
    (::spec/nuvlabox-created-id db)))

(reg-sub
  ::vpn-infra-options
  (fn [{:keys [::spec/vpn-infra]}]
    (map
      (fn [{:keys [id name]}] {:key id, :text name, :value id})
      vpn-infra)))
