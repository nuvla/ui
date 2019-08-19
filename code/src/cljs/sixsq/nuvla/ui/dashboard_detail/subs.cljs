(ns sixsq.nuvla.ui.dashboard-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.dashboard-detail.spec :as spec]
    [sixsq.nuvla.ui.dashboard.utils :as dashboard-utils]))


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
  ::jobs-per-page
  (fn [db]
    (::spec/jobs-per-page db)))


(reg-sub
  ::job-page
  (fn [db]
    (::spec/job-page db)))


(reg-sub
  ::deployment-parameters
  (fn [db]
    (->> db
         ::spec/deployment-parameters
         (into (sorted-map)))))


(reg-sub
  ::url
  :<- [::deployment-parameters]
  (fn [deployment-parameters [_ url-pattern]]
    (when (dashboard-utils/running-replicas? deployment-parameters)
      (dashboard-utils/resolve-url-pattern url-pattern deployment-parameters))))
