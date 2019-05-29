(ns sixsq.nuvla.ui.dashboard-detail.views-operations
  (:require
    [re-frame.core :refer [dispatch]]
    [sixsq.nuvla.ui.dashboard-detail.events :as events]
    [sixsq.nuvla.ui.utils.general :as general]
    [sixsq.nuvla.ui.utils.resource-details :as resource-details]))


;; Explicit keys have been added to the operation buttons to avoid react
;; errors for duplicate keys, which may happen when the data contains :key.
;; It is probably a bad idea to have a first argument that can be a map
;; as this will be confused with reagent options.
(defn operation-button [{:keys [id] :as data} description [label href operation-uri]]
  (case label
    "edit" ^{:key "edit"} [resource-details/edit-button data description #(dispatch [::events/edit id %])]
    "delete" ^{:key "delete"} [resource-details/delete-button data #(dispatch [::events/delete id])]
    ^{:key operation-uri} [resource-details/other-button label data #(dispatch [::events/operation id operation-uri])]))


(defn format-operations [refresh-button {:keys [operations] :as data} base-uri description]
  (let [ops (map (juxt #(general/operation-name (:rel %)) #(str base-uri (:href %)) :rel) operations)]
    (vec (concat [refresh-button] (map (partial operation-button data description) ops)))))
