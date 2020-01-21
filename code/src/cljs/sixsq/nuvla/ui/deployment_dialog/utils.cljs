(ns sixsq.nuvla.ui.deployment-dialog.utils
  (:require [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.time :as time]))


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


(defn credential-last-check-ago
  [{:keys [last-check] :as credential} locale]
  (some-> last-check time/parse-iso8601 (time/ago locale)))


(defn credential-status-valid
  [{:keys [status] :as credential}]
  (some-> status (= "VALID")))


(defn credential-is-outdated?
  [{:keys [last-check] :as credential}]
  (boolean
    (or (nil? last-check)
        (-> last-check
            time/parse-iso8601
            time/delta-minutes
            (> 5)))))


(defn credential-can-op-check?
  [credential]
  (general-utils/can-operation? "check" credential))


(defn credential-need-check?
  [credential]
  (boolean
    (and
      credential
      (credential-can-op-check? credential)
      (or (not (credential-status-valid credential))
          (credential-is-outdated? credential)))))
