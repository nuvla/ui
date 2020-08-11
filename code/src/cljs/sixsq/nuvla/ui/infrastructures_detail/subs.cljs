(ns sixsq.nuvla.ui.infrastructures-detail.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.infrastructures-detail.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::infrastructure-service
  (fn [db]
    (::spec/infrastructure-service db)))


(reg-sub
  ::can-edit?
  :<- [::infrastructure-service]
  (fn [infrastructure-service _]
    (general-utils/can-edit? infrastructure-service)))


(reg-sub
  ::can-delete?
  :<- [::infrastructure-service]
  (fn [infrastructure-service _]
    (general-utils/can-delete? infrastructure-service)))


(reg-sub
  ::can-terminate?
  :<- [::infrastructure-service]
  (fn [infrastructure-service _]
    (general-utils/can-terminate? infrastructure-service)))



(reg-sub
  ::can-stop?
  :<- [::infrastructure-service]
  (fn [infrastructure-service _]
    (general-utils/can-stop? infrastructure-service)))


(reg-sub
  ::can-start?
  :<- [::infrastructure-service]
  (fn [infrastructure-service _]
    (general-utils/can-start? infrastructure-service)))
