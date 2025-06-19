(ns sixsq.nuvla.ui.pages.edges-detail.subs
  (:require [clojure.string :as str]
            [goog.crypt.base64 :as b64]
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
  ::coe-resources
  :<- [::nuvlabox-status]
  :-> :coe-resources)

(reg-sub
  ::docker
  :<- [::coe-resources]
  :-> :docker)

(reg-sub
  ::coe-resource-docker-available?
  :-> ::spec/coe-resource-docker-available?)

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
    (assoc doc :RawMounts Mounts :Mounts new-Mounts)))

(defn update-network-settings
  [doc]
  (update doc :NetworkSettings (comp #(map name %) keys :Networks)))

(defn update-coe-resource-id
  [doc]
  (assoc doc :Id (:ID doc)))

(defn update-coe-resource-name
  [doc]
  (assoc doc :Name (-> doc :Spec :Name)))

(defn update-coe-resource-version
  [doc]
  (assoc doc :Version (-> doc :Version :Index)))

(defn update-coe-resource-labels
  [{{:keys [Labels]} :Spec :as doc}]
  (assoc doc :Labels Labels))

(defn update-node-role
  [{{:keys [Role]} :Spec :as doc}]
  (assoc doc :Role Role))

(defn update-node-availability
  [{{:keys [Availability]} :Spec :as doc}]
  (assoc doc :Availability Availability))

(defn update-node-hostname
  [{{:keys [Hostname]} :Description :as doc}]
  (assoc doc :Hostname Hostname))

(defn update-node-platform
  [{{:keys [Platform]} :Description :as doc}]
  (assoc doc :Platform (str (:Architecture Platform) " " (:OS Platform))))

(defn update-node-engine-version
  [{{:keys [Engine]} :Description :as doc}]
  (assoc doc :EngineVersion (:EngineVersion Engine)))

(defn update-node-engine-plugins
  [{{:keys [Engine]} :Description :as doc}]
  (assoc doc :EnginePlugins (reduce (fn [m {:keys [Name Type]}] (assoc m Name Type)) {} (:Plugins Engine))))

(defn update-node-memory
  [{{:keys [Resources]} :Description :as doc}]
  (assoc doc :Memory (some-> (:MemoryBytes Resources) (/ 1000000) int)))

(defn update-node-cpus
  [{{:keys [Resources]} :Description :as doc}]
  (assoc doc :CPUs (some-> (:NanoCPUs Resources) (/ 1000000000))))

(defn update-service-status
  [{:keys [ServiceStatus] :as doc}]
  (assoc doc
    :CompletedTasks (str (:CompletedTasks ServiceStatus) "/" (:DesiredTasks ServiceStatus))
    :RunningTasks (str (:RunningTasks ServiceStatus) "/" (:DesiredTasks ServiceStatus))))

(defn update-service-image
  [doc]
  (assoc doc :Image (-> doc :Spec :Labels :com.docker.stack.image)))

(defn update-service-namespace
  [doc]
  (assoc doc :Namespace (-> doc :Spec :Labels :com.docker.stack.namespace)))

(defn update-service-ports
  [{{:keys [Ports]} :Endpoint :as doc}]
  (let [new-Ports (map #(when-let [published-port (:PublishedPort %)]
                          (str (:PublishMode %) " " published-port "->" (:TargetPort %) "/" (:Protocol %))) Ports)]
    (assoc doc :Ports (remove nil? new-Ports))))

(defn update-service-virtual-ips
  [{{:keys [VirtualIPs]} :Endpoint :as doc}]
  (let [new-VirtualIPs (map :Addr VirtualIPs)]
    (assoc doc :VirtualIPs (remove nil? new-VirtualIPs))))

(defn update-service-replicas
  [{{:keys [Mode]} :Spec :as doc}]
  (assoc doc :Replicas (-> Mode :Replicated :Replicas)))

(defn update-config-data
  [{{:keys [Data]} :Spec :as doc}]
  (let [decoded-data (b64/decodeString Data)]
    (assoc doc :Data decoded-data)))

(defn update-task-state
  [{{:keys [State]} :Status :as doc}]
  (assoc doc :State State))

(defn update-task-container
  [{{:keys [ContainerStatus]} :Status :as doc}]
  (assoc doc :Container (:ContainerID ContainerStatus)))

(defn update-task-pid
  [{{:keys [ContainerStatus]} :Status :as doc}]
  (assoc doc :PID (:PID ContainerStatus)))

(defn update-task-exit-code
  [{{:keys [ContainerStatus]} :Status :as doc}]
  (assoc doc :ExitCode (:ExitCode ContainerStatus)))

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
             #(update % :IPAM (comp first :Config))]
            [:nodes ::docker-nodes
             #(-> % update-coe-resource-id update-coe-resource-name update-coe-resource-version update-coe-resource-labels
                  update-node-role update-node-availability update-node-hostname update-node-platform
                  update-node-memory update-node-cpus update-node-engine-version update-node-engine-plugins)]
            [:services ::docker-services
             #(-> % update-coe-resource-id update-coe-resource-name update-coe-resource-version update-coe-resource-labels
                  update-service-image update-service-namespace update-service-ports update-service-virtual-ips
                  update-service-replicas update-service-status)]
            [:tasks ::docker-tasks
             #(-> % update-coe-resource-id update-coe-resource-version update-task-state
                  update-task-container update-task-pid update-task-exit-code)]
            [:configs ::docker-configs
             #(-> % update-coe-resource-id update-coe-resource-name update-coe-resource-version update-coe-resource-labels
                  update-config-data)]
            [:secrets ::docker-secrets
             #(-> % update-coe-resource-id update-coe-resource-name update-coe-resource-version update-coe-resource-labels)]])

(defn containers-using-image
  [image containers]
  (filter #(= (-> image :raw :Id) (:ImageID %)) containers))

(reg-sub
  ::docker-images-with-usage-check
  :<- [::docker-images]
  :<- [::docker-containers]
  (fn [[images containers]]
    (mapv #(assoc % :InUse (not (empty? (containers-using-image % containers)))) images)))

(defn containers-using-volume
  [volume containers]
  (filter (fn [{:keys [RawMounts]}]
            (some #(and (= "volume" (:Type %)) (= (:Name volume) (:Name %)))
                  RawMounts)) containers))

(reg-sub
  ::docker-volumes-with-usage-check
  :<- [::docker-volumes]
  :<- [::docker-containers]
  (fn [[volumes containers]]
    (mapv #(assoc % :InUse (not (empty? (containers-using-volume % containers)))) volumes)))

(defn tasks-using-config
  [config tasks]
  (filter (fn [{:keys [Spec]}]
            (some #(= (:ID config) (:ConfigID %))
                  (-> Spec :ContainerSpec :Configs))) tasks))

(reg-sub
  ::docker-configs-with-usage-check
  :<- [::docker-configs]
  :<- [::docker-tasks]
  (fn [[configs tasks]]
    (mapv #(assoc % :InUse (not (empty? (tasks-using-config % tasks)))) configs)))

(defn tasks-using-secret
  [secret tasks]
  (filter (fn [{:keys [Spec]}]
            (some #(= (:ID secret) (:SecretID %))
                  (-> Spec :ContainerSpec :Secrets))) tasks))

(reg-sub
  ::docker-secrets-with-usage-check
  :<- [::docker-secrets]
  :<- [::docker-tasks]
  (fn [[secrets tasks]]
    (mapv #(assoc % :InUse (not (empty? (tasks-using-secret % tasks)))) secrets)))

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
  :-> ::spec/coe-resource-k8s-available?)

(reg-sub
  ::container-stats
  :<- [::nuvlabox-status]
  :-> (comp :container-stats :resources))

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
  ::installation-parameters
  :<- [::nuvlabox-status]
  :-> :installation-parameters)

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

(reg-sub
  ::registries
  (fn [db]
    (get-in db [::spec/registries])))

(reg-sub
  ::registries-options
  :<- [::registries]
  (fn [registries [_]]
    (map (fn [{:keys [id name endpoint]}]
           {:key   id
            :value id
            :text  (cond-> (or name id) endpoint (str " (" endpoint ")"))})
         registries)))

(reg-sub
  ::registries-credentials
  (fn [db]
    (group-by :parent (::spec/registries-credentials db))))

(reg-sub
  ::registries-credentials-options
  :<- [::registries-credentials]
  (fn [registries-credentials [_ registry-id]]
    (let [creds (get registries-credentials registry-id [])]
      (cons {:key "", :value nil, :text ""}
            (map (fn [{:keys [id name]}] {:key id, :value id, :text (or name id)}) creds)))))


