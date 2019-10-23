(ns sixsq.nuvla.ui.dashboard-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.dashboard-detail.spec :as spec]
    [sixsq.nuvla.ui.dashboard.utils :as dashboard-utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::deployment
  ::spec/deployment)


(reg-sub
  ::deployment-acl
  :<- [::deployment]
  (fn [deployment]
    (:acl deployment)))


(reg-sub
  ::deployment-module
  :<- [::deployment]
  (fn [deployment]
    (:module deployment)))


(reg-sub
  ::deployment-module-content
  :<- [::deployment-module]
  (fn [module]
    (:content module)))


(reg-sub
  ::is-deployment-application?
  :<- [::deployment-module]
  (fn [module]
    (= (:subtype module) "application")))


(reg-sub
  ::deployment-services-list
  :<- [::is-deployment-application?]
  :<- [::deployment-module-content]
  (fn [[is-application? {:keys [docker-compose]}]]
    (if is-application?
      (let [yaml (try
                   (general-utils/yaml->obj docker-compose)
                   (catch :default _))]
        (some-> yaml
                js->clj
                (get "services" {})
                keys
                sort))
      ["machine"])))


(reg-sub
  ::is-read-only?
  :<- [::deployment]
  (fn [deployment]
    (not (general-utils/can-edit? deployment))))


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


(reg-sub
  ::deployment-log
  (fn [db]
    (::spec/deployment-log db)))


(reg-sub
  ::deployment-log-id
  (fn [db]
    (::spec/deployment-log-id db)))


(reg-sub
  ::deployment-log-service
  (fn [db]
    (::spec/deployment-log-service db)))


(reg-sub
  ::deployment-log-since
  (fn [db]
    (::spec/deployment-log-since db)))


(reg-sub
  ::deployment-log-play?
  (fn [db]
    (::spec/deployment-log-play? db)))
