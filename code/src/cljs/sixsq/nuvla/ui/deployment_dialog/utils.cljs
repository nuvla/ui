(ns sixsq.nuvla.ui.deployment-dialog.utils
  (:require [taoensso.timbre :as log]))


(defn matches-parameter-name?
  [parameter-name parameter]
  (and parameter-name (= parameter-name (:parameter parameter))))


(defn update-parameter-in-list
  [name value parameters]
  (let [f (partial matches-parameter-name? name)
        current (first (filter f parameters))               ;; FIXME: Use group-by instead?
        others (remove f parameters)]
    (if current
      (->> (assoc current :value value)
           (conj others)
           (sort-by :parameter)
           vec))))


(defn update-parameter-in-deployment
  [name value deployment]
  (->> deployment
       :module
       :content
       :inputParameters
       (update-parameter-in-list name value)
       (assoc-in deployment [:module :content :inputParameters])))

(defn kw->str
  "Convert a keyword to a string, retaining any namespace."
  [kw]
  (subs (str kw) 1))


(defn data-records->mounts
  [data-records]
  (log/error "data-records->mounts data-records" data-records )
  (->> data-records
       :resources
       (map (fn [data-record]
              [((keyword "data:nfsIP") data-record)
               ((keyword "data:nfsDevice") data-record)
               ((keyword "data:bucket") data-record)]))
       distinct
       (map (fn [[ip device bucket]]
              (str "type=volume,volume-opt=o=addr=" ip
                   ",volume-opt=device=:" device
                   ",volume-opt=type=nfs,dst=/mnt/" bucket)))))

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


