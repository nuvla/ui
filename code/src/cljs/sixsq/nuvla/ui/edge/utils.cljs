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


(def nuvlabox-states [state-new
                      state-activated
                      state-commissioned
                      state-decommissioning
                      state-decommissioned
                      state-error])


(defn state->icon
  [state]
  (let [icons-map {state-activated       "handshake"
                   state-new             "dolly"
                   state-commissioned    "check"
                   state-decommissioning "eraser"
                   state-decommissioned  "trash"
                   state-error           "exclamation"}]
    (get icons-map state)))


(defn state-filter
  [state]
  (str "state='" state "'"))


(def default-params {:first 1, :last 10000})

(def floating-time-tolerance "-10s")

(def stale-nb-machines (assoc default-params :filter (str "nextCheck < 'now" floating-time-tolerance "'")
                                             :select "nuvlabox"))


(def active-nb-machines (assoc default-params :filter (str "nextCheck >= 'now" floating-time-tolerance "'")
                                              :select "nuvlabox"))


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
  [cpu]
  {:label      "CPU"
   :percentage cpu
   :value      (str cpu "%")})


(defn ram-stats
  [{:keys [capacity used] :as ram}]
  (let [percent (percentage used capacity)]
    {:label      (str "RAM (" (mb->gb capacity) " GB)")
     :percentage percent
     :value      (str percent "% - " (mb->gb capacity) " GB")}))


(defn disk-stats
  [[partition {:keys [capacity used] :as disk}]]
  (let [percent (percentage used capacity)]
    {:label      (str (str/lower-case (name partition)) " partition (" (mb->gb capacity) " GB)")
     :percentage percent
     :value      (str percent "% - " (mb->gb capacity) " GB")}))


(defn disks-stats
  [disks]
  (mapv disk-stats (sort-by #(-> % first name) disks)))


(defn load-statistics
  [{:keys [cpu ram disks] :as nb-detail}]
  (vec
    (concat [(cpu-stats cpu)]
            [(ram-stats ram)]
            (disks-stats disks))))
