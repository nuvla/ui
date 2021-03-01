(ns sixsq.nuvla.ui.edge.utils
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.utils.general :as general-utils]))

(def state-new "NEW")
(def state-activated "ACTIVATED")
(def state-commissioned "COMMISSIONED")
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

(defn state->icon
  "Commissioning state"
  [state]
  (let [icons-map {state-activated       "fas fa-handshake"
                   state-new             "fas fa-dolly"
                   state-commissioned    "fas fa-check"
                   state-decommissioning "fas fa-eraser"
                   state-decommissioned  "fas fa-trash"
                   state-error           "fas fa-exclamation"}]
    (get icons-map state)))


(defn status->icon
  [status]
  (let [icons-map {status-online   "fas fa-power-off"
                   status-offline  "fas fa-power-off"
                   status-unknown  "fas fa-question"}]
    (get icons-map status)))


(defn status->keyword
  [online]
  (case online
    true :online
    false :offline
    :unknown))


(defn status->color
  [status]
  (case status
    :online "green"
    :offline "red"
    :unknown "yellow"
    nil))


(defn operational-status->color
  [status]
  (case status
    "OPERATIONAL" "green"
    "DEGRADED" "red"
    "UNKNOWN" "yellow"
    nil))


(defn map-status->color
  [status]
  (case status
    :online "green"
    :offline "red"
    :unknown "orange"
    nil))


(defn state-filter
  [state]
  (str "state='" state "'"))



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
  [{:keys [capacity used topic raw-sample] :as ram}]
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
   :title (str "Total Network Stats per Interface")
   :tx    (map (fn [t] (/ t 1000000)) (map :bytes-transmitted net-stats))
   :rx    (map (fn [t] (/ t 1000000)) (map :bytes-received net-stats))})


(defn load-statistics
  [{:keys [cpu ram disks]}]
  (concat [(cpu-stats cpu)
           (ram-stats ram)]
          (map disk-stats (sort-by :device disks))))


(defn get-query-params
  [full-text-search page elements-per-page state-selector]
  {:first   (inc (* (dec page) elements-per-page))
   :last    (* page elements-per-page)
   :orderby "created:desc"
   :filter  (general-utils/join-and
              (when state-selector (state-filter state-selector))
              (general-utils/fulltext-query-string full-text-search))})


(defn get-query-summary-params
  [full-text-search]
  {:first       0
   :last        0
   :aggregation "terms:state"
   :filter      (general-utils/fulltext-query-string full-text-search)})


(defn prepare-compose-files
  [nuvlabox-release selected-peripherals replacements]
  (let [nuvlabox-file-scopes (group-by :scope (:compose-files nuvlabox-release))]
    (map
      (fn [peripheral]
        (let [{:keys [name file]} (first (get nuvlabox-file-scopes peripheral))
              replacement-list (partition 2 replacements)]
          {:name name
           :file (reduce #(apply str/replace %1 %2) file replacement-list)}))
      selected-peripherals)))


(defn get-major-version
  [full-version]
  (-> (str/split full-version #"\.") first))