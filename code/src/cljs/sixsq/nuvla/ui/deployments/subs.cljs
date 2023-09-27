(ns sixsq.nuvla.ui.deployments.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.deployments.spec :as spec]
            [sixsq.nuvla.ui.deployments.utils :as utils]))

(reg-sub
  ::deployments
  (fn [db]
    (::spec/deployments db)))

(reg-sub
  ::deployments-resources
  :<- [::deployments]
  :-> :resources)

(reg-sub
 ::deployment-edges
 :-> ::spec/deployment-edges)

(reg-sub
 ::deployment-edges-stati
 :<- [::deployment-edges]
 (fn [edges [_ id]]
   (-> edges (get id) :online)))

(reg-sub
  ::deployments-summary
  (fn [db]
    (::spec/deployments-summary db)))

(reg-sub
  ::deployments-summary-all
  (fn [db]
    (::spec/deployments-summary-all db)))

(reg-sub
  ::additional-filter
  (fn [db]
    (::spec/additional-filter db)))


(reg-sub
  ::view
  (fn [db]
    (::spec/view db)))

(reg-sub
  ::deployments-params-map
  (fn [db]
    (::spec/deployments-params-map db)))

(reg-sub
  ::deployment-url
  :<- [::deployments-params-map]
  (fn [deployments-params-map [_ id url-pattern]]
    (let [deployment-params-by-name (->> (get deployments-params-map id)
                                         (map (juxt :name identity))
                                         (into {}))]
      (when (utils/running-replicas? deployment-params-by-name)
        (utils/resolve-url-pattern url-pattern deployment-params-by-name)))))

(reg-sub
  ::state-selector
  (fn [db]
    (::spec/state-selector db)))


(reg-sub
  ::bulk-update-modal
  (fn [db]
    (::spec/bulk-update-modal db)))

(reg-sub
  ::bulk-stop-modal
  :-> ::spec/bulk-stop-modal)

(reg-sub
  ::bulk-delete-modal
  :-> ::spec/bulk-delete-modal)


(reg-sub
  ::deployments-count
  :<- [::deployments]
  (fn [deployments]
    (get deployments :count 0)))

(reg-sub
  ::deployments-without-edit-rights
  :-> ::spec/deployments-without-edit-rights)

(reg-sub
  ::deployments-sets-ids->names
  :-> ::spec/depl-set-ids->names)

(reg-sub
  ::deployment-set-name
  :<- [::deployments-sets-ids->names]
  (fn [depl-set-ids->names [_ id]]
    (depl-set-ids->names id)))
