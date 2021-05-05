(ns sixsq.nuvla.ui.deployment-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.deployment-detail.spec :as spec]
    [sixsq.nuvla.ui.deployment.utils :as deployment-utils]
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
  ::is-deployment-application-kubernetes?
  :<- [::deployment-module]
  (fn [module]
    (= (:subtype module) "application_kubernetes")))


(defn parse-application-yaml
  [docker-compose]
  (when-let [yaml (try
                    (general-utils/yaml->obj docker-compose)
                    (catch :default _))]
    (js->clj yaml)))


(reg-sub
  ::deployment-services-list
  :<- [::is-deployment-application?]
  :<- [::is-deployment-application-kubernetes?]
  :<- [::deployment-module-content]
  (fn [[is-application? is-application-kubernetes? {:keys [docker-compose]}]]
    (cond
      is-application? (some-> docker-compose
                              parse-application-yaml
                              first
                              (get "services" {})
                              keys
                              sort)
      is-application-kubernetes? (some->> docker-compose
                                          parse-application-yaml
                                          (map #(let [kind      (get % "kind")
                                                      meta-name (get-in % ["metadata" "name"])]
                                                  (str kind "/" meta-name)))
                                          sort)
      :else ["machine"])))


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
  ::deployment-parameters
  (fn [db]
    (->> db
         ::spec/deployment-parameters
         (into (sorted-map)))))


(reg-sub
  ::url
  :<- [::deployment-parameters]
  (fn [deployment-parameters [_ url-pattern]]
    (when (deployment-utils/running-replicas? deployment-parameters)
      (deployment-utils/resolve-url-pattern url-pattern deployment-parameters))))


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


(reg-sub
  ::module-versions
  (fn [db]
    (let [versions (::spec/module-versions db)]
      (apps-utils/map-versions-index versions))))


(reg-sub
  ::current-module-content-id
  :<- [::deployment-module-content]
  (fn [deployment-module-content]
    (:id deployment-module-content)))

(reg-sub
  ::current-module-version
  :<- [::module-versions]
  :<- [::current-module-content-id]
  (fn [[module-versions id]]
    (apps-utils/find-current-version module-versions id)))


(reg-sub
  ::upcoming-invoice
  (fn [db]
    (::spec/upcoming-invoice db)))


(reg-sub
  ::active-tab-index
  (fn [db]
    (get-in db [::spec/active-tab-index])))
