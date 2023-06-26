(ns sixsq.nuvla.ui.deployments-detail.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.deployments-detail.spec :as spec]
            [sixsq.nuvla.ui.deployments.utils :as deployments-utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::deployment
  (fn [db]
    (::spec/deployment db)))


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
  (fn [{:keys [subtype]}]
    (= subtype apps-utils/subtype-application)))


(reg-sub
  ::is-deployment-application-kubernetes?
  :<- [::deployment-module]
  (fn [{:keys [subtype]}]
    (= subtype apps-utils/subtype-application-k8s)))


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
  ::deployment-parameters
  (fn [db]
    (->> db
         ::spec/deployment-parameters
         (into (sorted-map)))))


(reg-sub
  ::url
  :<- [::deployment-parameters]
  (fn [deployment-parameters [_ url-pattern]]
    (when (deployments-utils/running-replicas? deployment-parameters)
      (deployments-utils/resolve-url-pattern url-pattern deployment-parameters))))


(reg-sub
  ::module-versions
  (fn [db]
    (let [versions (::spec/module-versions db)]
      (apps-utils/map-versions-index versions))))

(reg-sub
  ::module-versions-options
  :<- [::module-versions]
  :<- [::i18n-subs/tr]
  (fn [[versions-indexed tr]]
    (apps-utils/versions-options versions-indexed tr)))

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
  ::not-found?
  (fn [db]
    (::spec/not-found? db)))
