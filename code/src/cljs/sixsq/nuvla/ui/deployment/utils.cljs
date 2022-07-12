(ns sixsq.nuvla.ui.deployment.utils
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.deployment.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.values :as values]))

(def ^:const STARTED "STARTED")
(def ^:const STARTING "STARTING")
(def ^:const STOPPED "STOPPED")
(def ^:const ERROR "ERROR")

(defn state->icon
  [state]
  (let [icons-map {STARTED  "fas fa-play"
                   STARTING "fas fa-spinner"
                   STOPPED  "fas fa-stop"
                   ERROR    "fas fa-exclamation"}]
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
  (str "deployment/" (general-utils/id->uuid id)))

(defn state-filter
  [state]
  (if (= state STARTING)
    "state='RUNNING' or state='PENDING' or state='CREATED'"
    (str "state='" state "'")))

(defn get-filter-param
  [{:keys [full-text-search additional-filter state-selector nuvlabox module-id]
    :as   _args}]
  (let [filter-state     (when state-selector (state-filter state-selector))
        filter-nuvlabox  (when nuvlabox (str "nuvlabox='" nuvlabox "'"))
        filter-module-id (when module-id (str "module/id='" module-id "'"))
        full-text-search (general-utils/fulltext-query-string full-text-search)]
    (general-utils/join-and
      "id!=null"
      filter-state
      filter-nuvlabox
      filter-module-id
      full-text-search
      additional-filter)))

(defn get-query-params
  [{:keys [full-text-search additional-filter
           state-selector nuvlabox module-id page elements-per-page]}]
  (let [filter-str (get-filter-param
                     {:full-text-search  full-text-search
                      :additional-filter additional-filter
                      :state-selector    state-selector
                      :nuvlabox          nuvlabox
                      :module-id         module-id})]
    {:first       (inc (* (dec page) elements-per-page))
     :last        (* page elements-per-page)
     :aggregation "terms:state"
     :orderby     "created:desc"
     :filter      filter-str}))

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

(defn CloudNuvlaEdgeLink
  [{:keys [parent nuvlabox nuvlabox-name credential-name
           infrastructure-service infrastructure-service-name] :as _deployment}
   & {:keys [link] :or {link true}}]
  (let [href  (or nuvlabox infrastructure-service parent)
        label (or nuvlabox-name
                  infrastructure-service-name
                  credential-name
                  (some-> href general-utils/id->short-uuid))]
    (when href
      [:<>
       [ui/Icon {:name (cond nuvlabox "box"
                             infrastructure-service "cloud"
                             parent "key"
                             :else nil)}]
       (if link
         [values/as-link (general-utils/id->uuid href)
          :label label
          :page (cond nuvlabox "edges"
                      infrastructure-service "clouds"
                      parent "api/credential"
                      :else nil)]
         label)])))

(defn build-bulk-filter
  [{:keys [::spec/select-all?
           ::spec/selected-set
           ::spec/full-text-search
           ::spec/additional-filter
           ::spec/state-selector
           ::spec/nuvlabox]}]
  (if select-all?
    (get-filter-param
      {:full-text-search  full-text-search
       :additional-filter additional-filter
       :state-selector    (when-not (= "all" state-selector) state-selector)
       :nuvlabox          nuvlabox
       :module-id         nil})
    (->> selected-set
         (map #(str "id='" % "'"))
         (apply general-utils/join-or))))

(defn deployment-version
  [{:keys [module] :as _deployment}]
  (str "v"
       (apps-utils/find-current-version
         (apps-utils/map-versions-index (:versions module))
         (-> module :content :id))))
