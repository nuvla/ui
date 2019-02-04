(ns sixsq.slipstream.webui.deployment-detail.views-operations
  (:require
    [re-frame.core :refer [dispatch]]
    [sixsq.slipstream.webui.deployment-detail.events :as events]
    [sixsq.slipstream.webui.utils.general :as general]
    [sixsq.slipstream.webui.utils.resource-details :as resource-details]
    [taoensso.timbre :as log]))


;; Explicit keys have been added to the operation buttons to avoid react
;; errors for duplicate keys, which may happen when the data contains :key.
;; It is probably a bad idea to have a first argument that can be a map
;; as this will be confused with reagent options.
(defn operation-button [{:keys [id] :as data} description [label href operation-uri]]
  (case label
    "edit" ^{:key "edit"} [resource-details/edit-button data description #(dispatch [::events/edit id %])]
    "delete" ^{:key "delete"} [resource-details/delete-button data #(dispatch [::events/delete id])]
    ^{:key operation-uri} [resource-details/other-button label data #(dispatch [::events/operation id operation-uri])]))


(defn format-operations [refresh-button {:keys [operations] :as data} baseURI description]
  (let [ops (map (juxt #(general/operation-name (:rel %)) #(str baseURI (:href %)) :rel) operations)]
    (vec (concat [refresh-button] (map (partial operation-button data description) ops)))))
