(ns sixsq.nuvla.ui.edge.utils
  (:require
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.cimi-api.effects :refer [CLIENT]]
    [sixsq.nuvla.ui.utils.general :as general-utils]))

(def state-new "NEW")
(def state-activated "ACTIVATED")
(def state-commissioned "COMMISSIONED")
(def state-decommissioning "DECOMMISSIONING")
(def state-decommissioned "DECOMMISSIONED")
(def state-error "ERROR")


(defn state->icon
  [state]
  (let [icons-map {state-activated       "handshake"
                   state-new             "dolly"
                   state-commissioned    "check"
                   state-decommissioning "eraser"
                   state-decommissioned  "trash"
                   state-error           "exclamation"}]
    (get icons-map state)))


(defn status->color
  [status]
  (case status
    :online "green"
    :offline "red"
    :unknown "yellow"
    nil))


(defn map-status->color
  [status]
  (case status
    :online "green"
    :offline "red"
    :unknown "orange"
    nil))


(def filter-offline-status (str "next-heartbeat < 'now'"))

(def filter-online-status (str "next-heartbeat >= 'now'"))


(defn state-filter
  [state]
  (str "state='" state "'"))


(def default-params {:first 1, :last 10000})

(defn nuvlabox-status-search
  [params]
  (api/search @CLIENT :nuvlabox-status (general-utils/prepare-params params)))


(defn percentage
  [used capacity]
  (-> used
    (/ capacity)
    (* 100)
    int))


(defn mb->gb
  [mb]
  (cl-format nil "~,1F" (/ mb 1000.)))


(defn cpu-stats
  [{:keys [capacity load topic raw-sample]}]
  {:label      ["load" "free"]
   :title      (str capacity "-core CPU (%)")
   :percentage load
   :value      (- 100 load)
   :data-gateway  [topic raw-sample]})


(defn ram-stats
  [{:keys [capacity used topic raw-sample] :as ram}]
  (let [percent (percentage used capacity)]
    {:label     ["used" "free"]
     :title     (str (mb->gb capacity) " GB of RAM (%)" )
     :percentage percent
     :value      (- 100 percent)
     :data-gateway  [topic raw-sample]}))


(defn disk-stats
  [{:keys [device capacity used topic raw-sample]}]
  (let [percent (percentage used capacity)]
    {:label      ["used" "free"]
     :title      (str device " partition: " capacity " GB (%)")
     :percentage percent
     :value      (- 100 percent)
     :data-gateway      [topic raw-sample]}))


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


(defn prepare-compose-files
  [nuvlabox-release selected-peripherals nuvlabox-id]
  (let [nuvlabox-file-scopes (group-by :scope (:compose-files nuvlabox-release))]
    (map
      (fn [peripheral]
        (let [{:keys [name file]} (first (get nuvlabox-file-scopes peripheral))]
          {:name name
           :file (str/replace file #"\$\{NUVLABOX_UUID\}" nuvlabox-id)}))
      selected-peripherals)))


(defn get-major-version
  [full-version]
  (-> (str/split full-version #"\.") first))