(ns sixsq.nuvla.ui.common-components.deployment-dialog.utils
  (:require [sixsq.nuvla.ui.pages.clouds.utils :as clouds-utils]))


(defn kw->str
  "Convert a keyword to a string, retaining any namespace."
  [kw]
  (subs (str kw) 1))


;;
;; may want to consider the following implementation for invert-dataset-map
;;

(defn conj-dataset
  "Inserts 'dataset' into the list of datasets for the key 'offer' in the map
   result."
  [dataset-id result offer-id]
  (update-in result [(keyword offer-id)] conj dataset-id))


(defn entry-reducer
  "Merges into 'result' the inversion of a single entry in the dataset map."
  [result [dataset-kw offer-ids]]
  (let [f (->> dataset-kw
               kw->str
               (partial conj-dataset))]
    (reduce f result offer-ids)))


(defn invert-dataset-map
  [dataset-map]
  (reduce entry-reducer {} dataset-map))


(defmulti step-content identity)


(defn merge-module-element
  [key-fn current-val-fn current resolved]
  (let [coll->map           (fn [val-fn coll] (into {} (map (juxt key-fn val-fn) coll)))
        resolved-params-map (coll->map identity resolved)
        valid-params-set    (set (map key-fn resolved))
        current-params-map  (->> current
                                 (filter (fn [entry] (valid-params-set (key-fn entry))))
                                 (coll->map current-val-fn))]
    (into [] (vals (merge-with merge resolved-params-map current-params-map)))))


(defn merge-module
  [{current-content :content :as _current-module}
   {resolved-content :content :as resolved-module}
   module-version-href]
  (let [params (merge-module-element :name #(select-keys % [:value])
                                     (:output-parameters current-content)
                                     (:output-parameters resolved-content))
        env    (merge-module-element :name #(select-keys % [:value])
                                     (:environmental-variables current-content)
                                     (:environmental-variables resolved-content))

        files  (merge-module-element :file-name #(select-keys % [:file-content])
                                     (:files current-content)
                                     (:files resolved-content))]
    (assoc resolved-module
      :content
      (cond-> (dissoc resolved-content :output-parameters :environmental-variables :files)
              (seq params) (assoc :output-parameters params)
              (seq env) (assoc :environmental-variables env)
              (seq files) (assoc :files files))
      :href module-version-href)))

(defn infra-app-compatible?
  [{app-subtype :subtype app-compatibility :compatibility :as _module}
   {infra-subtype :subtype :as infra-service}]
  (boolean (not (and (= infra-subtype "swarm")
                     (= app-subtype "application")
                     (= app-compatibility "swarm")
                     (or (clouds-utils/swarm-disabled? infra-service)
                         (clouds-utils/swarm-worker? infra-service))))))

(defn infra-app-unmet-requirements
  [architectures
   {:keys [min-cpu min-ram min-disk] :as _minimum-requirements}
   edge-architecture
   {{cpu-capacity :capacity} :cpu,
    {ram-capacity :capacity} :ram,
    disks                    :disks :as _edge-resources}]
  (let [available-disk-space (apply max (cons 0 (map #(- (or (:capacity %) 0) (or (:used %) 0)) disks)))]

    (cond-> {}
            (and edge-architecture (some-> architectures set (contains? edge-architecture) not))
            (assoc :architecture {:supported         architectures
                                  :edge-architecture edge-architecture})

            (and cpu-capacity min-cpu (< cpu-capacity min-cpu))
            (assoc :cpu {:min min-cpu, :available cpu-capacity})

            (and ram-capacity min-ram (< ram-capacity min-ram))
            (assoc :ram {:min min-ram, :available ram-capacity})

            (and available-disk-space min-disk (< available-disk-space min-disk))
            (assoc :disk {:min min-disk, :available available-disk-space}))))

(defn infra-app-min-requirements-met?
  [architectures minimum-requirements edge-architecture edge-resources]
  (or (nil? minimum-requirements)
      (empty? (infra-app-unmet-requirements architectures minimum-requirements edge-architecture edge-resources))))
