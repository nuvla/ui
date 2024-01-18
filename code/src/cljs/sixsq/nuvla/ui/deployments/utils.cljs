(ns sixsq.nuvla.ui.deployments.utils
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.deployments.spec :as spec]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.values :as values]))

(def ^:const CREATED "CREATED")
(def ^:const STARTED "STARTED")
(def ^:const STARTING "STARTING")
(def ^:const STOPPED "STOPPED")
(def ^:const ERROR "ERROR")
(def ^:const PENDING "PENDING")
(def ^:const UPDATING "UPDATING")

(defn state->icon
  [state]
  (let [icons-map {STARTED  icons/i-circle-play
                   STARTING icons/i-loader
                   STOPPED  icons/i-circle-stop
                   ERROR    icons/i-triangle-exclamation
                   PENDING  icons/i-sync}]
    (get icons-map state)))

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

(defn started?
  [state]
  (= state STARTED))

(defn stopped?
  [state]
  (= state STOPPED))

(defn deployment-href
  [id]
  (name->href routes/deployment-details {:uuid (general-utils/id->uuid id)}))

(defn state-filter
  [state]
  (case state
    ("all" nil "TOTAL") nil
    STARTING "state='RUNNING' or state='PENDING' or state='CREATED'"
    PENDING "state='RUNNING' or state='PENDING' or state='STOPPING'"
    (str "state='" state "'")))

(defn get-filter-param
  [{:keys [full-text-search additional-filter state-selector filter-external]}]
  (let [filter-state (state-filter state-selector)]
    (general-utils/join-and
      "id!=null"
      filter-state
      filter-external
      full-text-search
      additional-filter)))

(defn build-bulk-filter
  ([{:keys [::spec/additional-filter
            ::spec/state-selector
            ::spec/select] :as db}]
   (table-plugin/build-bulk-filter
     select
     (get-filter-param
       {:full-text-search (full-text-search-plugin/filter-text
                            db [::spec/deployments-search])
        :additional-filter additional-filter
        :state-selector   state-selector
        :module-id        nil}))))


(defn get-query-params-summary
  [{:keys [full-text-search additional-filter external-filter]}]
  (let [filter-str (general-utils/join-and
                     full-text-search additional-filter external-filter)
        aggregate  "terms:state"]
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

(defn CloudNuvlaEdgeLink
  [{:keys [parent nuvlabox nuvlabox-name credential-name
           infrastructure-service infrastructure-service-name] :as _deployment}
   & {:keys [link color] :or {link true}}]
  (let [href  (or nuvlabox infrastructure-service parent)
        label (or nuvlabox-name
                infrastructure-service-name
                credential-name
                (some-> href general-utils/id->short-uuid))]
    (when href
      [:<>
       (cond nuvlabox [icons/BoxIcon {:color color}]
             infrastructure-service [icons/CloudIcon]
             parent [icons/KeyIcon]
             :else nil)
       (if link
         [values/AsLink (general-utils/id->uuid href)
          :label label
          :page (cond nuvlabox "edges"
                      infrastructure-service "clouds"
                      parent "api/credential"
                      :else nil)]
         label)])))

(defn get-version-number
  [versions content]
  (some-> versions
    apps-utils/map-versions-index
    (apps-utils/find-current-version (:id content))))

(defn deployment-version
  [{{:keys [versions content]} :module :as _deployment}]
  (when-let [version (get-version-number versions content)]
    (str "v" version)))
