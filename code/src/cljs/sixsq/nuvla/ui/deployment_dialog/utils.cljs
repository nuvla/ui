(ns sixsq.nuvla.ui.deployment-dialog.utils)


(defn matches-env-name?
  [env-name env-variable]
  (and env-name (= env-name (:name env-variable))))


(defn update-env-variable-in-list
  [env-name env-value env-variables]
  (let [f       (partial matches-env-name? env-name)
        current (first (filter f env-variables))            ;; FIXME: Use group-by instead?
        others  (remove f env-variables)]
    (if current
      (->> (assoc current :value env-value)
           (conj others)
           vec))))


(defn update-env-variable-in-deployment
  [env-name env-value deployment]
  (->> deployment
       :module
       :content
       :environmental-variables
       (update-env-variable-in-list env-name env-value)
       (assoc-in deployment [:module :content :environmental-variables])))

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


