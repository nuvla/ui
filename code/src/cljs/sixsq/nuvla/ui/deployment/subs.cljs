(ns sixsq.nuvla.ui.deployment.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.deployment.spec :as spec]
    [sixsq.nuvla.ui.deployment.utils :as utils]))


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
  (fn [deployments-params-map [_ {:keys [id module] :as deployment}]]
    (let [deployment-params-by-name (->> (get deployments-params-map id)
                                         (map (juxt :name identity))
                                         (into {}))
          first-url (first (get-in module[:content :urls]))
          url-name (first first-url)
          url-pattern (second first-url)
          url (utils/resolve-url-pattern url-pattern deployment-params-by-name)]
      [url-name url])))
