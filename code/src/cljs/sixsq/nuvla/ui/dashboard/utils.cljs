(ns sixsq.nuvla.ui.dashboard.utils
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.time :as time]))


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
         (every? true?))                      ;; careful, this returns true for an empty collection!
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


(defn detail-href
  [id]
  (str "dashboard/" (general-utils/id->uuid id)))


(defn get-query-params
  [full-text-search active-only? page elements-per-page]
  (let [filter-active-only? (when active-only? "state!='STOPPED'")
        full-text-search    (when-not (str/blank? full-text-search)
                              (str "fulltext=='" full-text-search "*'"))
        filter              (str/join " and " (remove nil? [filter-active-only? full-text-search]))]
    (cond-> {:first   (inc (* (dec page) elements-per-page))
             :last    (* page elements-per-page)
             :orderby "created:desc"}
            (not (str/blank? filter)) (assoc :filter filter))))
