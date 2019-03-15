(ns sixsq.nuvla.ui.deployment-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.deployment-detail.spec :as spec]
    [sixsq.nuvla.ui.deployment.utils :as deployment-utils]))


(reg-sub
  ::reports
  ::spec/reports)

(reg-sub
  ::loading?
  ::spec/loading?)


(reg-sub
  ::deployment
  ::spec/deployment)


(reg-sub
  ::events
  (fn [db]
    (::spec/events db)))


(reg-sub
  ::jobs
  (fn [db]
    (::spec/jobs db)))


(reg-sub
  ::deployment-parameters
  (fn [db]
    (::spec/deployment-parameters db)))



(reg-sub
  ::url
  :<- [::deployment-parameters]
  (fn [deployment-parameters [_ url-pattern]]
    (deployment-utils/resolve-url-pattern url-pattern deployment-parameters)))
