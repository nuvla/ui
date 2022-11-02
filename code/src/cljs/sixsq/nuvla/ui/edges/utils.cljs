(ns sixsq.nuvla.ui.edges.utils
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.time :as time]))

(def state-new "NEW")
(def state-activated "ACTIVATED")
(def state-commissioned "COMMISSIONED")
(def state-suspended "SUSPENDED")
(def state-decommissioning "DECOMMISSIONING")
(def state-decommissioned "DECOMMISSIONED")
(def state-error "ERROR")

(def status-online "ONLINE")
(def status-offline "OFFLINE")
(def status-unknown "UNKNOWN")

(def vuln-critical-color "#f41906")
(def vuln-high-color "#f66e0a")
(def vuln-medium-color "#fbbc06")
(def vuln-low-color "#21b802")
(def vuln-unknown-color "#949494")

(def orchestration-icons
  {:swarm      "docker"
   :kubernetes "/ui/images/kubernetes.svg"})

(defn state->icon
  "Commissioning state"
  [state]
  (let [icons-map {state-activated       "fas fa-handshake"
                   state-new             "fas fa-dolly"
                   state-commissioned    "fas fa-check"
                   state-decommissioning "fas fa-eraser"
                   state-decommissioned  "fas fa-trash"
                   state-suspended       "fas fa-pause"
                   state-error           "fas fa-exclamation"}]
    (get icons-map state)))


(defn status->keyword
  [online]
  (case online
    true :online
    false :offline
    :unknown))


(defn status->color
  [status]
  (case status
    true "green"
    false "red"
    "yellow"))


(defn operational-status->color
  [status]
  (case status
    "OPERATIONAL" "green"
    "DEGRADED" "orange"
    "UNKNOWN" "yellow"
    nil))


(defn map-status->color
  [status]
  (case status
    :online "green"
    :offline "red"
    :unknown "orange"
    nil))


(defn map-online->color
  [status]
  (case status
    true (map-status->color :online)
    false (map-status->color :offline)
    nil (map-status->color :unknown)
    (map-status->color nil)))


(defn state-filter
  "Build a filter according to selected state. The default case condition corresponds
   to the commissioning state, while the first three correspond to the online state."
  [state]
  (case state
    "ONLINE" "online=true"
    "OFFLINE" "online=false"
    "UNKNOWN" "online=null"
    (str "state='" state "'")))



(defn cpu-stats
  [{:keys [capacity load topic raw-sample]}]
  (let [percent (-> (general-utils/percentage load capacity)
                    (general-utils/round-up))]
    {:label        ["load [last 15 min]" "free"]
     :title        (str capacity "-core CPU (load %)")
     :percentage   percent
     :value        (- 100 percent)
     :data-gateway (if (nil? topic) [] [topic raw-sample])}))


(defn ram-stats
  [{:keys [capacity used topic raw-sample] :as _ram}]
  (let [percent         (-> (general-utils/percentage used capacity)
                            (general-utils/round-up :n-decimal 0))
        capacity-gbytes (-> (general-utils/mbytes->gbytes capacity)
                            (general-utils/round-up))]
    {:label        ["used" "free"]
     :title        (str capacity-gbytes " GB of RAM (%)")
     :percentage   percent
     :value        (- 100 percent)
     :data-gateway (if (nil? topic) [] [topic raw-sample])}))


(defn disk-stats
  [{:keys [device capacity used topic raw-sample]}]
  (let [percent (-> (general-utils/percentage used capacity)
                    (general-utils/round-up :n-decimal 0))]
    {:label        ["used" "free"]
     :title        (str device " partition: " capacity " GB (%)")
     :percentage   percent
     :value        (- 100 percent)
     :data-gateway (if (nil? topic) [] [topic raw-sample])}))


(defn load-net-stats
  [net-stats]
  {:label (map :interface net-stats)
   :title (str "Cumulative Network Stats per Interface")
   :tx    (map (fn [t] (/ t 1000000)) (map :bytes-transmitted net-stats))
   :rx    (map (fn [t] (/ t 1000000)) (map :bytes-received net-stats))})


(defn load-statistics
  [{:keys [cpu ram disks]}]
  (concat [(cpu-stats cpu)
           (ram-stats ram)]
          (map disk-stats (sort-by :device disks))))


(defn get-query-aggregation-params
  [full-text-search aggregation extra]
  {:first       0
   :last        0
   :aggregation aggregation
   :filter      (general-utils/join-and
                  full-text-search
                  (when extra extra))})


(defn prepare-compose-files
  ([nuvlabox-release selected-peripherals]
   (prepare-compose-files nuvlabox-release selected-peripherals identity))
  ([nuvlabox-release selected-peripherals transform-fn]
   (let [nuvlabox-file-scopes (group-by :scope (:compose-files nuvlabox-release))]
     (map
       (fn [peripheral]
         (let [{:keys [name file]} (first (get nuvlabox-file-scopes peripheral))]
           {:name name
            :file (transform-fn file)}))
       selected-peripherals))))


(defn get-major-version
  [full-version]
  (-> (str/split full-version #"\.") first))

(defn format-update-data
  [form-data]
  (let [payload-releated   (select-keys form-data [:project-name :working-dir
                                                   :environment  :force-restart
                                                   :current-version])
        nuvlabox-release   (:nuvlabox-release form-data)
        available-modules  (:compose-files nuvlabox-release)
        module-scope->name (zipmap (map (comp keyword :scope) available-modules)
                                   (map :name available-modules))
        selected-modules   (remove nil? (:modules form-data))
        config-files         (into [] (concat ["docker-compose.yml"] (map #(get module-scope->name %) selected-modules)))
        payload?           (some (fn [[_ v]] (not (str/blank? v))) payload-releated)
        payload            (when payload?
                             (-> payload-releated
                                 (update :environment str/split #"\n")
                                 (assoc :config-files config-files)))]
    (when payload
        (assoc {:nuvlabox-release (:id nuvlabox-release)}
               :payload (general-utils/edn->json payload)))))


(defn form-update-data-incomplete?
  [{:keys [project-name working-dir environment] :as form-data}]
  (let [payload?            (->> [project-name working-dir environment]
                                 (some (complement str/blank?))
                                 boolean)
        payload-incomplete? (->> [project-name working-dir]
                                 (some str/blank?)
                                 boolean)]
    (or (str/blank? (:nuvlabox-release form-data))
        (and payload? payload-incomplete?))))


(defn form-add-playbook-incomplete?
  [{:keys [name run enabled type parent] :as _form-data}]
  (->> [name run enabled type parent]
       (some str/blank?)
       boolean))

(defn last-time-online [next-heartbeat-moment refresh-interval locale]
  (time/ago
          (->> refresh-interval
               (* 2)
               (+ 10)
               (* 1000)
               (time/subtract-milliseconds next-heartbeat-moment)
               ) locale))