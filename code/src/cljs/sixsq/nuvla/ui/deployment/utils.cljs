(ns sixsq.nuvla.ui.deployment.utils
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.time :as time]))


(def status-started "STARTED")
(def status-starting "STARTING")
(def status-stopped "STOPPED")
(def status-error "ERROR")


(defn status->icon
  [status]
  (let [icons-map {status-started  "fas fa-play"
                   status-starting "fas fa-spinner"
                   status-stopped  "fas fa-stop"
                   status-error    "fas fa-exclamation"}]
    (get icons-map status)))


(defn resolve-url-pattern
  "When url-pattern is passed and all used params in pattern has values in
  deployment-parameters it return a resolved url otherwise nil"
  [url-pattern deployment-parameters]
  (when url-pattern
    (let [pattern-in-params (re-seq #"\$\{([^}]+)\}+" url-pattern)
          pattern-value     (map (fn [[param-pattern param-name]]
                                   (some->> (get deployment-parameters param-name)
                                            :value
                                            (conj [param-pattern])))
                                 pattern-in-params)]
      (when (every? some? pattern-value)
        (reduce
          (fn [url [param-pattern param-value]]
            (str/replace url param-pattern param-value))
          url-pattern pattern-value)))))


(defn is-replicas-running?
  "Select all strings that end with 'replicas.running'."
  [[k _]]
  (and (string? k) (str/ends-with? k "replicas.running")))


(defn positive-number?
  "Determines if the value is a positive number. Will not throw an exception
   if the argument is not a number."
  [n]
  (and (number? n) (pos? n)))


(defn is-value-positive?
  [entry]
  (->> entry
       second
       :value
       general-utils/str->int
       positive-number?))


(defn running-replicas?
  "Extracts the number of running replicas and returns true if the number is
   positive. Returns false is all other cases."
  [deployment-parameters]
  (if (seq deployment-parameters)
    (->> deployment-parameters
         (filter is-replicas-running?)
         (map is-value-positive?)
         (every? true?))                                    ;; careful, this returns true for an empty collection!
    false))


(defn deployment-in-transition?
  [state]
  (str/ends-with? (str state) "ING"))


(defn assoc-delta-time
  "Given the start (as a string), this adds a :delta-time entry in minutes."
  [start {end :timestamp :as evt}]
  (assoc evt :delta-time (time/delta-minutes start end)))


(defn is-started?
  [state]
  (= state "STARTED"))


(defn deployment-href
  [id]
  (str "deployment/" (general-utils/id->uuid id)))


(defn state-filter
  [state]
  (case state
    "STARTING" "state='RUNNING' or state='PENDING' or state='CREATED'"
    (str "state='" state "'")))


(defn get-filter-param
  [full-text-search additional-filter state-selector nuvlabox]
  (let [filter-state     (when state-selector (state-filter state-selector))
        filter-nuvlabox  (when nuvlabox (str "nuvlabox='" nuvlabox "'"))
        full-text-search (general-utils/fulltext-query-string full-text-search)]
    (general-utils/join-and
      filter-state
      filter-nuvlabox
      full-text-search
      additional-filter)))


(defn get-query-params
  [full-text-search additional-filter state-selector nuvlabox page elements-per-page]
  (let [filter-str (get-filter-param full-text-search additional-filter
                                     state-selector nuvlabox)]
    (cond-> {:first       (inc (* (dec page) elements-per-page))
             :last        (* page elements-per-page)
             :aggregation "terms:state"
             :orderby     "created:desc"}
            (not (str/blank? filter-str)) (assoc :filter filter-str))))


(defn get-query-params-summary
  [full-text-search additional-filter]
  (let [full-text-search (general-utils/fulltext-query-string full-text-search)
        filter-str       (general-utils/join-and full-text-search additional-filter)
        aggregate        "terms:state"]
    (cond-> {:orderby     "created:desc"
             :aggregation aggregate
             :first       0
             :last        0}
            (not (str/blank? filter-str)) (assoc :filter filter-str))))


(defn is-selected?
  [selected-set id]
  (contains? selected-set id))


(defn visible-deployment-ids
  [deployments]
  (->> deployments
       :resources
       (map :id)
       set))


(defn all-page-selected?
  [selected-set visible-deps-ids-set]
  (set/superset? selected-set visible-deps-ids-set))
