(ns sixsq.nuvla.ui.edge-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.edge-detail.spec :as spec]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.time :as time]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::nuvlabox-status
  (fn [db]
    (::spec/nuvlabox-status db)))


(reg-sub
  ::nuvlabox-online-status
  :<- [::nuvlabox-status]
  (fn [{:keys [online]}]
    (utils/status->keyword online)))


(reg-sub
  ::nuvlabox-vulns
  (fn [db]
    (::spec/nuvlabox-vulns db)))


(reg-sub
  ::nuvlabox-associated-ssh-keys
  (fn [db]
    (::spec/nuvlabox-associated-ssh-keys db)))


(reg-sub
  ::nuvlabox-peripherals
  (fn [db]
    (::spec/nuvlabox-peripherals db)))


(reg-sub
  ::nuvlabox-peripherals-ids
  :<- [::nuvlabox-peripherals]
  (fn [nuvlabox-peripherals]
    (keys nuvlabox-peripherals)))


(reg-sub
  ::nuvlabox-peripheral
  :<- [::nuvlabox-peripherals]
  (fn [nuvlabox-peripherals [_ id]]
    (get nuvlabox-peripherals id)))


(reg-sub
  ::nuvlabox-events
  (fn [db]
    (::spec/nuvlabox-events db)))

(reg-sub
  ::elements-per-page
  (fn [db]
    (::spec/elements-per-page db)))

(reg-sub
  ::page
  (fn [db]
    (::spec/page db)))

(reg-sub
  ::vuln-severity-selector
  (fn [db]
    (::spec/vuln-severity-selector db)))

(reg-sub
  ::matching-vulns-from-db
  (fn [db]
    (::spec/matching-vulns-from-db db)))

(reg-sub
  ::next-heartbeat-moment
  :<- [::nuvlabox-status]
  (fn [{:keys [next-heartbeat]}]
    (some-> next-heartbeat time/parse-iso8601)))


(reg-sub
  ::nuvlabox
  (fn [db]
    (::spec/nuvlabox db)))


(reg-sub
  ::can-decommission?
  :<- [::nuvlabox]
  (fn [nuvlabox _]
    (general-utils/can-operation? "decommission" nuvlabox)))


(reg-sub
  ::can-edit?
  :<- [::nuvlabox]
  (fn [nuvlabox _]
    (general-utils/can-edit? nuvlabox)))


(reg-sub
  ::can-delete?
  :<- [::nuvlabox]
  (fn [nuvlabox _]
    (general-utils/can-delete? nuvlabox)))


(reg-sub
  ::active-tab-index
  (fn [db]
    (get-in db [::spec/active-tab-index])))


(reg-sub
  ::nuvlabox-managers
  (fn [db]
    (::spec/nuvlabox-managers db)))


(reg-sub
  ::join-token
  (fn [db]
    (::spec/join-token db)))


(reg-sub
  ::nuvlabox-cluster
  (fn [db]
    (::spec/nuvlabox-cluster db)))


(reg-sub
  ::nuvlabox-not-found?
  (fn [db]
    (::spec/nuvlabox-not-found? db)))


(reg-sub
  ::nuvlabox-playbooks
  (fn [db]
    (::spec/nuvlabox-playbooks db)))


(reg-sub
  ::nuvlabox-emergency-playbooks
  (fn [db]
    (::spec/nuvlabox-emergency-playbooks db)))


(reg-sub
  ::nuvlabox-current-playbook
  (fn [db]
    (::spec/nuvlabox-current-playbook db)))


(reg-sub
  ::nuvlabox-log
  (fn [db]
    (::spec/nuvlabox-log db)))


(reg-sub
  ::nuvlabox-log-id
  (fn [db]
    (::spec/nuvlabox-log-id db)))


(reg-sub
  ::nuvlabox-log-since
  (fn [db]
    (::spec/nuvlabox-log-since db)))


(reg-sub
  ::nuvlabox-log-play?
  (fn [db]
    (::spec/nuvlabox-log-play? db)))
