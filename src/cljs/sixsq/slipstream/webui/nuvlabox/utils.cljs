(ns sixsq.slipstream.webui.nuvlabox.utils
  (:require
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [sixsq.slipstream.webui.cimi-api.utils :as cimi-api-utils]))


(def default-params {:$first 1, :$last 10000})

(def floating-time-tolerance "-10s")

(def stale-nb-machines (assoc default-params :$filter (str "nextCheck < 'now" floating-time-tolerance "'")
                                             :$select "nuvlabox"))


(def active-nb-machines (assoc default-params :$filter (str "nextCheck >= 'now" floating-time-tolerance "'")
                                              :$select "nuvlabox"))


(defn nuvlabox-search
  [client params]
  (cimi/search client "nuvlaboxStates" (cimi-api-utils/sanitize-params params)))


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
