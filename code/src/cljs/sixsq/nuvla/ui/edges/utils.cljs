(ns sixsq.nuvla.ui.edges.utils
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.edges.spec :as spec]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.time :as time]))

(def state-new "NEW")
(def state-activated "ACTIVATED")
(def state-commissioned "COMMISSIONED")
(def state-decommissioning "DECOMMISSIONING")
(def state-decommissioned "DECOMMISSIONED")
(def state-suspended "SUSPENDED")
(def state-error "ERROR")

(def states [state-new state-activated state-commissioned state-decommissioning
             state-decommissioned state-suspended state-error])

(def status-online "ONLINE")
(def status-offline "OFFLINE")
(def status-unknown "UNKNOWN")

(def vuln-critical-color "#f41906")
(def vuln-high-color "#f66e0a")
(def vuln-medium-color "#fbbc06")
(def vuln-low-color "#21b802")
(def vuln-unknown-color "#949494")

(def capability-heartbeat "NUVLA_HEARTBEAT")

(defn score-vulnerability
  [{:keys [vulnerability-score] :as item}]
  (let [set-fn #(assoc item :severity %1 :color %2)]
    (cond
      (nil? vulnerability-score) (set-fn "UNKNOWN" vuln-unknown-color)
      (>= vulnerability-score 9.0) (set-fn "CRITICAL" vuln-critical-color)
      (and (< vulnerability-score 9.0)
           (>= vulnerability-score 7.0)) (set-fn "HIGH" vuln-high-color)
      (and (< vulnerability-score 7.0)
           (>= vulnerability-score 4.0)) (set-fn "MEDIUM" vuln-medium-color)
      (< vulnerability-score 4.0) (set-fn "LOW" vuln-low-color))))

(def orchestration-icons
  {:swarm      icons/i-docker
   :kubernetes "/ui/images/kubernetes.svg"})

(defn state->icon
  "Commissioning state"
  [state]
  (let [icons-map {state-activated       icons/i-handshake
                   state-new             icons/i-dolly
                   state-commissioned    icons/i-check
                   state-decommissioning icons/i-eraser
                   state-decommissioned  icons/i-ban
                   state-suspended       icons/i-pause
                   state-error           icons/i-exclamation}]
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
  (let [payload-releated (select-keys form-data [:project-name :working-dir
                                                 :environment :force-restart
                                                 :current-version])
        nuvlabox-release (:nuvlabox-release form-data)
        selected-modules (->> (:modules form-data)
                              (filter val)
                              (map key)
                              (remove nil?))
        config-files     (concat ["docker-compose.yml"]
                                 (map #(str "docker-compose." (name %) ".yml") selected-modules))
        payload?         (some (fn [[_ v]] (not (str/blank? v))) payload-releated)
        payload          (when payload?
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

(defn edges-details-url
  [id]
  (name->href routes/edges-details {:uuid id}))

(defn get-full-filter-string
  [{:keys [::spec/state-selector
           ::spec/additional-filter
           ::spec/external-restriction-filter] :as db}]
  (general-utils/join-and
    "id!=null"
    (when state-selector (state-filter state-selector))
    additional-filter
    external-restriction-filter
    (full-text-search-plugin/filter-text
      db [::spec/edges-search])))

(defn build-bulk-filter
  [db-path db]
  (table-plugin/build-bulk-filter (get-in db db-path) (get-full-filter-string db)))

(defn- parse-version-number [v]
  (->> (re-seq #"\d+" (or v ""))
       (map js/Number)))

(defn compare-versions [m n]
  (let [[m_1 m_2 m_3] (parse-version-number m)
        [n_1 n_2 n_3] (parse-version-number n)]
    (cond
      (not= m_1 n_1) (compare n_1 m_1)
      (not= m_2 n_2) (compare n_2 m_2)
      (not= m_3 n_3) (compare n_3 m_3)
      :else 0)))

(defn sort-by-version [e]
  (sort-by :release compare-versions e))

(defn summary-stats [summary]
  (let [total           (:count summary)
        online-statuses (general-utils/aggregate-to-map
                          (get-in summary [:aggregations :terms:online :buckets]))
        online          (:1 online-statuses)
        offline         (:0 online-statuses)
        unknown         (- total (+ online offline))]
    {:total total :online online :offline offline :unknown unknown}))

(defn telemetry-outdated?
  [{:keys [next-telemetry] :as _nb-status}]
  (and (some? next-telemetry) (some-> next-telemetry
                                      time/parse-iso8601
                                      time/before-now?)))

(defn has-capability?
  [capability {:keys [capabilities] :as _nuvlabox}]
  (contains? (set capabilities) capability))

(def has-capability-heartbeat? (partial has-capability? capability-heartbeat))
