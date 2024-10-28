(ns sixsq.nuvla.ui.pages.edges-detail.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.pages.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.time :as time]))

(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))

(reg-sub
  ::nuvlabox-status
  :-> ::spec/nuvlabox-status)

(reg-sub
  ::nuvlabox-status-set-time
  :-> ::spec/nuvlabox-status-set-time)

(reg-sub
  ::show-telemetry-outdated?
  :<- [::nuvlabox-status]
  :<- [::nuvlabox-status-set-time]
  (fn [[nb-status _set-time]]
    ;; subscription to ::nuvlabox-status-set-time is only used to force re-evaluate
    ;; only when new nb-status document is being set
    (edges-utils/telemetry-outdated? nb-status)))

(reg-sub
  ::stats-container-ordering
  :-> ::spec/stats-container-ordering)

(reg-sub
  ::container-stats
  :<- [::nuvlabox-status]
  :-> (comp :container-stats :resources))

(reg-sub
  ::coe-resources
  :<- [::nuvlabox-status]
  :-> :coe-resources)

(reg-sub
  ::coe-resource-docker-available?
  :-> ::spec/coe-resource-docker-available?)

(reg-sub
  ::docker
  :<- [::coe-resources]
  :-> :docker)

(reg-sub
  ::kubernetes
  :<- [::coe-resources]
  :-> :kubernetes)

(reg-sub
  ::docker-images
  :<- [::docker]
  :-> :images)

(reg-sub
  ::k8s-images
  :<- [::kubernetes]
  :-> :images)

(reg-sub
  ::k8s-namespaces
  :<- [::kubernetes]
  :-> :namespaces)

(reg-sub
  ::k8s-pods
  :<- [::kubernetes]
  :-> :pods)

(reg-sub
  ::k8s-nodes
  :<- [::kubernetes]
  :-> :nodes)

(reg-sub
  ::k8s-nodes
  :<- [::kubernetes]
  :-> :nodes)

(reg-sub
  ::k8s-configmaps
  :<- [::kubernetes]
  :-> :configmaps)

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

(reg-sub
  ::docker-images-clean
  :<- [::docker-images]
  (fn [images]
    (map (fn [image]
           (-> image
               (update :Id str/replace #"^sha256:" "")
               update-created
               (dissoc :SharedSize :Containers))) images)))

(reg-sub
  ::k8s-namespaces-clean
  :<- [::k8s-namespaces]
  (fn [namespaces]
    (map (fn [{:keys [metadata] :as namespace}]
           (assoc namespace
             :uid (:uid metadata)
             :name (:name metadata)
             :creation_timestamp (:creation_timestamp metadata)
             :resource_version (:resource_version metadata))) namespaces)))

(reg-sub
  ::k8s-pods-clean
  :<- [::k8s-pods]
  (fn [pods]
    (map (fn [{:keys [metadata status] :as namespace}]
           (assoc namespace
             :uid (:uid metadata)
             :name (:name metadata)
             :creation_timestamp (:creation_timestamp metadata)
             :resource_version (:resource_version metadata)
             :namespace (:namespace metadata)
             :phase (:phase status))) pods)))

(reg-sub
  ::k8s-nodes-clean
  :<- [::k8s-nodes]
  (fn [pods]
    (map (fn [{:keys [metadata status] :as namespace}]
           (assoc namespace
             :uid (:uid metadata)
             :name (:name metadata)
             :creation_timestamp (:creation_timestamp metadata)
             :resource_version (:resource_version metadata)
             :node_info (:node_info status))) pods)))

(reg-sub
  ::k8s-configmaps-clean
  :<- [::k8s-configmaps]
  (fn [configmaps]
    (map (fn [{:keys [metadata] :as namespace}]
           (assoc namespace
             :uid (:uid metadata)
             :name (:name metadata)
             :creation_timestamp (:creation_timestamp metadata)
             :resource_version (:resource_version metadata)
             :namespace (:namespace metadata))) configmaps)))

(reg-sub
  ::docker-images-ordering
  :-> ::spec/docker-images-ordering)

(reg-sub
  ::docker-volumes
  :<- [::docker]
  :-> :volumes)

(reg-sub
  ::docker-containers
  :<- [::docker]
  :-> :containers)

(reg-sub
  ::docker-containers-clean
  :<- [::docker-containers]
  (fn [containers]
    (map (fn [container]
           (-> container
               update-ports
               update-created
               update-mounts
               update-network-settings)) containers)))

(reg-sub
  ::docker-networks
  :<- [::docker]
  :-> :networks)

(reg-sub
  ::docker-networks-clean
  :<- [::docker-networks]
  (fn [networks]
    (map #(update % :IPAM (comp first :Config)) networks)))

(reg-sub
  ::coe-resource-k8s-available?
  :-> ::spec/coe-resource-k8s-available?)

(reg-sub
  ::augmented-container-stats
  :<- [::container-stats]
  (fn [container-stats]
    (mapv (fn [{:keys [mem-usage mem-limit] :as row}]
            (assoc row :mem-usage-perc
                       (when (and (number? mem-usage) (number? mem-limit) (not (zero? mem-limit)))
                         (/ (double mem-usage) mem-limit))))
          container-stats)))

(reg-sub
  ::nuvlaedge-release
  :-> ::spec/nuvlaedge-release)

(defn- version-string->number-vec [version]
  (map js/Number (str/split version ".")))


(defn security-available? [version]
  (let [[major minor _] (version-string->number-vec version)]
    (and (<= 2 major)
         (<= 3 minor))))

(reg-sub
  ::nuvlabox-modules
  :<- [::nuvlabox-status]
  (fn [nuvlabox-status]
    (let [scope-regex-match        #".*docker-compose\.([a-z]+)\.yml$"
          version                  (:nuvlabox-engine-version nuvlabox-status)
          invalid-version-string?  (not (every? int? (take 2 (version-string->number-vec version))))
          version-with-sec-module? (security-available? version)
          config-files             (get-in nuvlabox-status [:installation-parameters :config-files])
          modules                  (remove nil?
                                           (map #(-> (re-matches scope-regex-match %)
                                                     second
                                                     keyword) config-files))
          modules->bool            (zipmap modules (cycle [true]))]
      (if (and (or version-with-sec-module?
                   invalid-version-string?)
               (nil? (modules->bool :security)))
        (assoc modules->bool :security false)
        modules->bool))))

(reg-sub
  ::nuvlabox-components
  :<- [::nuvlabox-status]
  (fn [{:keys [components]}]
    components))

(reg-sub
  ::nuvlabox-vulns
  (fn [{{:keys [items] :as vulns} ::spec/nuvlabox-vulns}]
    (assoc vulns :items (map edges-utils/score-vulnerability items))))

(reg-sub
  ::nuvlabox-associated-ssh-keys
  (fn [db]
    (::spec/nuvlabox-associated-ssh-keys db)))

(reg-sub
  ::nuvlabox-peripherals
  (fn [db]
    (::spec/nuvlabox-peripherals db)))

(reg-sub
  ::nuvlabox-peripherals-ids
  :<- [::nuvlabox-peripherals]
  (fn [nuvlabox-peripherals]
    (keys nuvlabox-peripherals)))

(reg-sub
  ::nuvlabox-peripheral
  :<- [::nuvlabox-peripherals]
  (fn [nuvlabox-peripherals [_ id]]
    (get nuvlabox-peripherals id)))

(reg-sub
  ::vuln-severity-selector
  (fn [db]
    (::spec/vuln-severity-selector db)))

(reg-sub
  ::matching-vulns-from-db
  (fn [db]
    (::spec/matching-vulns-from-db db)))

(reg-sub
  ::nuvlabox
  (fn [db]
    (::spec/nuvlabox db)))

(reg-sub
  ::ne-version
  :<- [::nuvlabox]
  :<- [::nuvlabox-status]
  (fn [[{nb-engine-version :nuvlabox-engine-version}
        {nb-status-engine-version :nuvlabox-engine-version}]]
    (or nb-status-engine-version nb-engine-version)))

(reg-sub
  ::can-decommission?
  :<- [::nuvlabox]
  (fn [nuvlabox]
    (general-utils/can-operation? "decommission" nuvlabox)))

(reg-sub
  ::can-edit?
  :<- [::nuvlabox]
  (fn [nuvlabox]
    (general-utils/can-edit? nuvlabox)))

(reg-sub
  ::can-update?
  :<- [::nuvlabox]
  (fn [nuvlabox]
    (general-utils/can-operation? "update-nuvlabox" nuvlabox)))

(reg-sub
  ::can-coe-resource-actions?
  :<- [::nuvlabox]
  (fn [nuvlabox]
    (general-utils/can-operation? "coe-resource-actions" nuvlabox)))

(reg-sub
  ::update-available?
  :<- [::can-update?]
  :<- [::nuvlabox-status]
  (fn [[can-update? nb-status]]
    (and can-update? nb-status)))

(reg-sub
  ::can-delete?
  :<- [::nuvlabox]
  (fn [nuvlabox]
    (general-utils/can-delete? nuvlabox)))

(reg-sub
  ::nuvlabox-managers
  (fn [db]
    (::spec/nuvlabox-managers db)))

(reg-sub
  ::join-token
  (fn [db]
    (::spec/join-token db)))

(reg-sub
  ::nuvlabox-cluster
  (fn [db]
    (::spec/nuvlabox-cluster db)))

(reg-sub
  ::nuvlabox-not-found?
  (fn [db]
    (::spec/nuvlabox-not-found? db)))

(reg-sub
  ::nuvlabox-playbooks
  (fn [db]
    (::spec/nuvlabox-playbooks db)))

(reg-sub
  ::infra-services
  (fn [db]
    (::spec/infra-services db)))

(reg-sub
  ::nuvlabox-emergency-playbooks
  (fn [db]
    (::spec/nuvlabox-emergency-playbooks db)))

(reg-sub
  ::nuvlabox-current-playbook
  (fn [db]
    (::spec/nuvlabox-current-playbook db)))

(reg-sub
  ::edge-stats
  :-> ::spec/edge-stats)

(reg-sub
  ::timespan
  :-> ::spec/timespan)

(reg-sub
  ::availability-15-min
  :-> ::spec/availability-15-min)

(reg-sub
  ::stats-table-current-cols
  :<- [::main-subs/current-cols spec/stats-table-col-configs-local-storage-key ::spec/stats-columns-ordering]
  identity)
