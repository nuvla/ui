(ns sixsq.nuvla.ui.cimi.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub]]
    [sixsq.nuvla.ui.cimi.events :as events]
    [sixsq.nuvla.ui.cimi.spec :as spec]
    [taoensso.timbre :as log]))


(reg-sub
  ::query-params
  ::spec/query-params)


(reg-sub
  ::aggregations
  ::spec/aggregations)


(reg-sub
  ::collection
  ::spec/collection)


(reg-sub
  ::collection-name
  ::spec/collection-name)


(reg-sub
  ::selected-fields
  ::spec/selected-fields)


(reg-sub
  ::available-fields
  ::spec/available-fields)


(reg-sub
  ::cloud-entry-point
  ::spec/cloud-entry-point)


(reg-sub
  ::show-add-modal?
  ::spec/show-add-modal?)


(reg-sub
  ::collections-templates-cache
  ::spec/collections-templates-cache)


(reg-sub
  ::collection-templates
  :<- [::collections-templates-cache]
  (fn [collections-templates-cache [_ template-href]]
    (when (contains? collections-templates-cache template-href)
      (if-let [templates-info (template-href collections-templates-cache)]
        templates-info
        (dispatch [::events/get-templates template-href])))))


(reg-sub
  ::loading?
  ::spec/loading?)


(reg-sub
  ::selected-rows
  (fn [{:keys [::spec/selected-rows]}]
    selected-rows))

(reg-sub
  ::row-selected?
  :<- [::selected-rows]
  (fn [selected-rows [_ id]]
    (contains? selected-rows id)))
