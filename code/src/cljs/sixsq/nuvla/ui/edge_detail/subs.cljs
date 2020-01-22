(ns sixsq.nuvla.ui.edge-detail.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.edge-detail.spec :as spec]
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
  ::nuvlabox-peripherals
  (fn [db]
    (::spec/nuvlabox-peripherals db)))


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
