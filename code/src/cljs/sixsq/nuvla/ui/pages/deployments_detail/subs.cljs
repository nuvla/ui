(ns sixsq.nuvla.ui.pages.deployments-detail.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.pages.deployments-detail.spec :as spec]
            [sixsq.nuvla.ui.pages.deployments.utils :as deployments-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.time :as time]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::deployment
  (fn [db]
    (::spec/deployment db)))

(reg-sub
  ::nuvlabox
  :-> ::spec/nuvlabox)


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
  ::is-deployment-docker-compose?
  :<- [::deployment-module]
  (fn [{:keys [compatibility]}]
    (= compatibility apps-utils/compatibility-docker-compose)))


(reg-sub
  ::is-deployment-application-kubernetes?
  :<- [::deployment-module]
  (fn [{:keys [subtype]}]
    (= subtype apps-utils/subtype-application-k8s)))

(reg-sub
  ::is-deployment-application-helm?
  :<- [::deployment-module]
  (fn [{:keys [subtype]}]
    (= subtype apps-utils/subtype-application-helm)))


(defn parse-application-yaml
  [docker-compose]
  (when-let [yaml (try
                    (general-utils/yaml->obj docker-compose)
                    (catch :default _))]
    (js->clj yaml)))

(def ^:const k8s-workload-object-kinds
  #{"CronJob"
    "DaemonSet"
    "Deployment"
    "Job"
    "Pod"
    "ReplicaSet"
    "StatefulSet"})

(defn only-k8s-workload-object-kinds
  [object]
  (when (contains? k8s-workload-object-kinds (get object "kind"))
    (let [kind (get object "kind")
          name (get-in object ["metadata" "name"])]
      (str kind "/" name))))

(reg-sub
  ::deployment-services-list
  :<- [::is-deployment-application?]
  :<- [::is-deployment-application-kubernetes?]
  :<- [::is-deployment-application-helm?]
  :<- [::deployment-module-content]
  (fn [[is-application? is-application-kubernetes? is-application-helm? {:keys [docker-compose]}]]
    (cond
      is-application? (some-> docker-compose
                              parse-application-yaml
                              first
                              (get "services" {})
                              keys
                              sort)
      is-application-kubernetes? (some->> docker-compose
                                          parse-application-yaml
                                          (map only-k8s-workload-object-kinds)
                                          (remove nil?)
                                          sort)
      is-application-helm? []
      :else [])))


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
  ::coe-resources
  :-> ::spec/coe-resources)

(reg-sub
  ::docker
  :<- [::coe-resources]
  (fn [coe-resources]
    (-> coe-resources :docker)))

(reg-sub
  ::coe-resource-docker-available?
  :<- [::docker]
  (fn [docker]
    (seq docker)))

(defn save-raw-data
  [resource]
  (assoc resource :raw resource))

(defn reg-sub-fn-coe-resource
  [coe-key sub-key-resource-key-list]
  (doseq [[resource-key sub-key clean-fn] sub-key-resource-key-list]
    (reg-sub
      sub-key
      :<- [coe-key]
      :-> (comp #(mapv (comp (or clean-fn identity) save-raw-data) %) resource-key))))

(reg-sub
  ::kubernetes
  :<- [::coe-resources]
  :-> :kubernetes)

(defn k8s-flat-metadata
  [{:keys [metadata] :as resource}]
  (assoc resource
    :uid (:uid metadata)
    :name (:name metadata)
    :creation_timestamp (:creation_timestamp metadata)
    :resource_version (:resource_version metadata)))

(defn k8s-namespace-metadata
  [{:keys [metadata] :as resource}]
  (assoc resource :namespace (:namespace metadata)))

(defn k8s-flat-metadata-namespace
  [resource]
  ((comp k8s-flat-metadata k8s-namespace-metadata) resource))

(reg-sub-fn-coe-resource
  ::kubernetes [[:images ::k8s-images]
                [:namespaces ::k8s-namespaces k8s-flat-metadata]
                [:pods ::k8s-pods
                 (fn [{:keys [status] :as resource}]
                   (-> (k8s-flat-metadata-namespace resource)
                       (assoc :phase (:phase status))))]
                [:nodes ::k8s-nodes
                 (fn [{:keys [status] :as resource}]
                   (-> (k8s-flat-metadata resource)
                       (assoc :node_info (:node_info status))))]
                [:configmaps ::k8s-configmaps k8s-flat-metadata-namespace]
                [:secrets ::k8s-secrets k8s-flat-metadata-namespace]
                [:statefulsets ::k8s-statefulsets k8s-flat-metadata-namespace]
                [:persistentvolumes ::k8s-persistentvolumes k8s-flat-metadata-namespace]
                [:persistentvolumeclaims ::k8s-persistentvolumeclaims k8s-flat-metadata-namespace]
                [:daemonsets ::k8s-daemonsets k8s-flat-metadata-namespace]
                [:deployments ::k8s-deployments k8s-flat-metadata-namespace]
                [:jobs ::k8s-jobs k8s-flat-metadata-namespace]
                [:ingresses ::k8s-ingresses k8s-flat-metadata-namespace]
                [:cronjobs ::k8s-cronjobs k8s-flat-metadata-namespace]
                [:services ::k8s-services k8s-flat-metadata-namespace]
                [:helmreleases ::k8s-helmreleases]])

(reg-sub
  ::coe-resource-k8s-available?
  :<- [::kubernetes]
  (fn [k8s]
    (seq k8s)))

(defn update-created
  [doc]
  (update doc :Created #(some-> % time/parse-unix time/time->utc-str)))

(defn update-ports
  [{:keys [Ports] :as doc}]
  (let [new-Ports (map #(when-let [public-port (:PublicPort %)]
                          (str (:IP %) ":" public-port "->" (:PrivatePort %) "/" (:Type %))) Ports)]
    (assoc doc :Ports (remove nil? new-Ports))))

(defn update-mounts
  [{:keys [Mounts] :as doc}]
  (let [new-Mounts (map #(str (if (= (:Type %) "volume")
                                (:Name %)
                                (:Source %))
                              ":" (:Destination %) ":" (if (:RW %) "rw" "ro")) Mounts)]
    (assoc doc :Mounts new-Mounts)))

(defn update-network-settings
  [doc]
  (update doc :NetworkSettings (comp #(map name %) keys :Networks)))

(reg-sub-fn-coe-resource
  ::docker [[:images ::docker-images
             (fn [image]
               (-> image
                   (update :Id str/replace #"^sha256:" "")
                   update-created
                   (dissoc :SharedSize :Containers)))]
            [:volumes ::docker-volumes]
            [:containers ::docker-containers
             #(-> % update-ports update-created update-mounts update-network-settings)]
            [:networks ::docker-networks
             #(update % :IPAM (comp first :Config))]])

(reg-sub
  ::not-found?
  (fn [db]
    (::spec/not-found? db)))
