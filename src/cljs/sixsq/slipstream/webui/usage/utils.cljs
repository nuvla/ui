(ns sixsq.slipstream.webui.usage.utils
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<! >! chan timeout]]
    [clojure.string :as str]
    [promesa.core :as p]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [sixsq.slipstream.webui.utils.time :as time]))


(def vms-unit "VMs [h]")
(def cpus-unit "CPUs [h]")
(def ram-unit "RAM [GB·h]")
(def disk-unit "DISK [GB·h]")
(def price-unit "PRICE [€]")

(def ^:const billable-filter "billable=true")

(def compute-aggregations (str/join ", " ["sum:serviceOffer/resource:vcpu"
                                          "sum:serviceOffer/resource:ram"
                                          "sum:serviceOffer/resource:disk"
                                          "sum:price"]))

;; hack to distinguish storage from compute metering records
(def vm-filter "(bucketName=null)")


(defn date-range
  "Provides a tuple that represents the start/end of a range of days relative
   to the current day. The arguments [0 0] would represent the start/end of the
   current day."
  [start end]
  [(time/days-before start) (.endOf (time/days-before end) "day")])


(def date-range-entries
  {"today"        (date-range 0 0)
   "yesterday"    (date-range 1 1)
   "last-7-days"  (date-range 7 1)
   "last-30-days" (date-range 30 1)
   "custom"       (date-range 30 1)})

(def default-date-range "last-30-days")


(defn get-date-range-tag
  [range]
  (or (ffirst (filter (fn [[_ v]] (time/range-equals range v)) date-range-entries)) "custom"))


(defn to-hour [v]
  (/ v 60))


(defn to-GB-from-MB [v]
  (/ v 1024))


(defn extract-compute-stats
  [response]
  {:vms   {:unit  vms-unit
           :value (-> response (get :count 0) to-hour)}
   :cpus  {:unit  cpus-unit
           :value (-> response
                      (get-in [:aggregations :sum:serviceOffer/resource:vcpu :value] 0)
                      to-hour)}
   :ram   {:unit  ram-unit
           :value (-> response
                      (get-in [:aggregations :sum:serviceOffer/resource:ram :value] 0)
                      to-GB-from-MB
                      to-hour)}
   :disk  {:unit  disk-unit
           :value (-> response
                      (get-in [:aggregations :sum:serviceOffer/resource:disk :value] 0)
                      to-hour)}
   :price {:unit  price-unit
           :value (get-in response [:aggregations :sum:price :value] 0)}})


(defn fetch-totals [client date-after date-before credentials billable-only?]
  (go
    (let [filter-created-str (str "snapshot-time>'" date-after "' and snapshot-time<'" date-before "'")
          filter-credentials (str/join " or " (map #(str "credentials/href='" % "'") credentials))
          filter-str (cond-> (str filter-created-str " and (" filter-credentials ")")
                             billable-only? (str " and " billable-filter)
                             true (str " and " vm-filter))
          request-opts {:$last        0
                        :$filter      filter-str
                        :$aggregation compute-aggregations}
          response (<! (cimi/search client "meterings" request-opts))]
      (extract-compute-stats response))))


(defn fetch-metering [callback client date-after date-before credential billable-only?]
  (go
    (let [filter-created-str (str "snapshot-time>'" date-after "' and snapshot-time<'" date-before "'")
          filter-credentials (str "credentials/href='" credential "'")
          filter-str (cond-> (str filter-created-str " and (" filter-credentials ")")
                             billable-only? (str " and " billable-filter)
                             true (str " and " vm-filter))
          request-opts {:$last        0
                        :$filter      filter-str
                        :$aggregation compute-aggregations}
          response (<! (cimi/search client "meterings" request-opts))]
      (callback
        [credential (extract-compute-stats response)]))))


(defn fetch-meterings [client
                       date-after
                       date-before
                       credentials
                       billable-only?
                       callback]
  (let [p (p/all (map #(p/promise (fn [callback _]
                                    (fetch-metering callback client date-after date-before % billable-only?))) credentials))]
    (p/then p #(->> % (into {}) callback))))
