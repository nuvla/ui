(ns sixsq.nuvla.ui.pages.edges-detail.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
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
  ::docker
  :<- [::coe-resources]
  :-> :docker)

(reg-sub
  ::docker-images
  :<- [::docker]
  :-> :images)

(defn update-created
  [doc]
  (update doc :Created #(some-> % time/parse-unix time/time->utc-str)))

(reg-sub
  ::docker-images-clean
  :<- [::docker-images]
  (fn [images]
    (map (fn [image]
           (-> image
               (assoc :id (str/replace (:Id image) #"^sha256:" ""))
               update-created
               (dissoc :Id :SharedSize :Containers))) images)))

(reg-sub
  ::docker-images-ordering
  :-> ::spec/docker-images-ordering)

(reg-sub
  ::docker-images-ordered
  :<- [::docker-images-clean]
  :<- [::docker-images-ordering]
  (fn [[images ordering]]
    (sort (partial general-utils/multi-key-direction-sort ordering) images)))

(reg-sub
  ::docker-volumes
  :<- [::docker]
  :-> :volumes)

(reg-sub
  ::docker-volumes-clean
  :<- [::docker-volumes]
  (fn [volumes]
    (map (fn [volume]
           (-> volume
               (assoc :id (:Name volume))
               (dissoc :Name))) volumes)))

(reg-sub
  ::docker-volumes-ordering
  :-> ::spec/docker-volumes-ordering)

(reg-sub
  ::docker-volumes-ordered
  :<- [::docker-volumes-clean]
  :<- [::docker-volumes-ordering]
  (fn [[volumes ordering]]
    (sort (partial general-utils/multi-key-direction-sort ordering) volumes)))

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
               (assoc :id (:Id container))
               update-created
               (dissoc :Id))) containers)))

(reg-sub
  ::docker-containers-ordering
  :-> ::spec/docker-containers-ordering)

(reg-sub
  ::docker-containers-ordered
  :<- [::docker-containers-clean]
  :<- [::docker-containers-ordering]
  (fn [[containers ordering]]
    (js/console.info ordering)
    (sort (partial general-utils/multi-key-direction-sort ordering) containers)))

(reg-sub
  ::docker-networks
  :<- [::docker]
  :-> :networks)

(reg-sub
  ::docker-networks-clean
  :<- [::docker-networks]
  (fn [networks]
    (map (fn [network]
           (-> network
               (assoc :id (:Id network))
               (dissoc :Id))) networks)))

(reg-sub
  ::docker-networks-ordering
  :-> ::spec/docker-networks-ordering)

(reg-sub
  ::docker-networks-ordered
  :<- [::docker-networks-clean]
  :<- [::docker-networks-ordering]
  (fn [[networks ordering]]
    (sort (partial general-utils/multi-key-direction-sort ordering) networks)))

;(reg-sub
;  ::docker-configs
;  :<- [::docker]
;  :-> :configs)
;
;(reg-sub
;  ::docker-configs-clean
;  :<- [::docker-configs]
;  (fn [configs]
;    (map (fn [config]
;           (-> config
;               (assoc :id (:ID config)
;                      :Name (get-in config [:Spec :Name])
;                      :Data (.atob js/window (get-in config [:Spec :Data]))
;                      :Labels (get-in config [:Spec :Labels])
;                      :Version (get-in config [:Version :Index]))
;               (dissoc :Spec :ID))) configs)))
;
;(reg-sub
;  ::docker-configs-ordering
;  :-> ::spec/docker-configs-ordering)
;
;(reg-sub
;  ::docker-configs-ordered
;  :<- [::docker-configs-clean]
;  :<- [::docker-configs-ordering]
;  (fn [[configs ordering]]
;    (sort (partial general-utils/multi-key-direction-sort ordering) configs)))

(reg-sub
  ::container-stats-ordered
  :<- [::container-stats]
  :<- [::stats-container-ordering]
  (fn [[container-stats stats-container-ordering]]
    (sort (partial general-utils/multi-key-direction-sort stats-container-ordering) container-stats)))

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
