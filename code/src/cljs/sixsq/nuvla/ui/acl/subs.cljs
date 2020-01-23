(ns sixsq.nuvla.ui.acl.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.acl.events :as events]
    [sixsq.nuvla.ui.acl.spec :as spec]))


(reg-sub
  ::users-and-groups
  (fn [{:keys [::spec/users-and-groups] :as db}]
    users-and-groups))

(reg-sub
  ::principal-name
  :<- [::users-and-groups]
  (fn [users-and-groups [_ principal]]
    (if-let [principal-details (get users-and-groups principal)]
      principal-details
      (dispatch [::events/get-principal principal]))))


(reg-sub
  ::users-options
  (fn [{:keys [::spec/users-options] :as db}]
    users-options))


(reg-sub
  ::groups-options
  (fn [{:keys [::spec/groups-options] :as db}]
    groups-options))
