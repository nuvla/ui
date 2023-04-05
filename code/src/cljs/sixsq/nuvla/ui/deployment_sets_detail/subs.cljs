(ns sixsq.nuvla.ui.deployment-sets-detail.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(reg-sub
  ::loading?
  :-> ::spec/loading?)

(reg-sub
  ::deployment-set
  :-> ::spec/deployment-set)

(reg-sub
  ::can-edit?
  :<- [::deployment-set]
  (fn [deployment-set]
    (general-utils/can-edit? deployment-set)))

(reg-sub
  ::can-delete?
  :<- [::deployment-set]
  (fn [deployment-set]
    (general-utils/can-delete? deployment-set)))

(reg-sub
  ::deployment-set-not-found?
  :-> ::spec/deployment-set-not-found?)

(reg-sub
  ::targets-selected
  (fn [db [_ i]]
    (vals (get-in db [::spec/apps-sets i ::spec/targets-selected]))))

(reg-sub
  ::get
  (fn [db [_ k]]
    (get db k)))

(reg-sub
  ::module-applications-sets
  :-> ::spec/module-applications-sets)

(reg-sub
  ::applications-sets
  :<- [::module-applications-sets]
  :-> (comp :applications-sets :content))
