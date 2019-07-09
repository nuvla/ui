(ns sixsq.nuvla.ui.dashboard.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.dashboard.spec :as spec]
    [sixsq.nuvla.ui.dashboard.utils :as utils]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::deployments
  (fn [db]
    (::spec/deployments db)))


(reg-sub
  ::elements-per-page
  (fn [db]
    (::spec/elements-per-page db)))


(reg-sub
  ::page
  (fn [db]
    (::spec/page db)))


(reg-sub
  ::active-only?
  (fn [db]
    (::spec/active-only? db)))


(reg-sub
  ::creds-name-map
  (fn [db]
    (::spec/creds-name-map db)))


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
      (utils/resolve-url-pattern url-pattern deployment-params-by-name))))
