(ns sixsq.nuvla.ui.edge-detail.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.edge-detail.spec :as spec]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.time :as time]
    [taoensso.timbre :as log]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::nuvlabox-status
  (fn [db]
    (::spec/nuvlabox-status db)))


(reg-sub
  ::next-heartbeat-moment
  :<- [::nuvlabox-status]
  (fn [{:keys [next-heartbeat]}]
    (some-> next-heartbeat time/parse-iso8601)))


(reg-sub
  ::status-nuvlabox
  :<- [::next-heartbeat-moment]
  (fn [next-heartbeat-moment _]
    (if next-heartbeat-moment
      (if (time/after-now? next-heartbeat-moment)
        :online
        :offline)
      :unknown)))


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
  ::can-delete?
  :<- [::nuvlabox]
  (fn [nuvlabox _]
    (general-utils/can-delete? nuvlabox)))
