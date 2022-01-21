(ns sixsq.nuvla.ui.cimi.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.cimi.events :as events]
    [sixsq.nuvla.ui.cimi.spec :as spec]
    [sixsq.nuvla.ui.filter-comp.utils :as utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-sub
  ::query-params
  (fn [db]
    (::spec/query-params db)))


(reg-sub
  ::orderby-map
  :<- [::query-params]
  (fn [{:keys [orderby]}]
    (some->>
      (str/split orderby #"\s*,\s*")
      (remove str/blank?)
      (map #(let [[label sort-direction] (str/split % #":")] [label (or sort-direction "asc")]))
      (into {}))))


(reg-sub
  ::orderby-label-icon
  :<- [::orderby-map]
  (fn [orderby-map [_ label]]
    (let [sort-direction (get orderby-map label)
          direction      (case sort-direction
                           "asc" " ascending"
                           "desc" " descending"
                           "")]
      (str "sort" direction))))



(reg-sub
  ::aggregations
  ::spec/aggregations)


(reg-sub
  ::collection
  (fn [db]
    (::spec/collection db)))


(reg-sub
  ::can-bulk-delete?
  :<- [::collection]
  (fn [collection]
    (general-utils/can-bulk-delete? collection)))


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
  (fn [db]
    (::spec/cloud-entry-point db)))


(reg-sub
  ::base-uri
  :<- [::cloud-entry-point]
  (fn [cloud-entry-point]
    (:base-uri cloud-entry-point)))


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


(reg-sub
  ::resources-metadata-map
  (fn [db]
    (::spec/resource-metadata db)))


(reg-sub
  ::resource-metadata
  :<- [::resources-metadata-map]
  (fn [resources-metadata-map [_ resource-name]]
    (get resources-metadata-map resource-name)))


(reg-sub
  ::resource-metadata-attributes
  (fn [[_ resource-name]] (subscribe [::resource-metadata resource-name]))
  (fn [resource-metadata]
    (utils/cimi-metadata-simplifier resource-metadata)))


(reg-sub
  ::resource-metadata-input-parameters
  (fn [[_ resource-name operation]] [(subscribe [::resource-metadata resource-name])
                                     (atom operation)])
  (fn [[resource-metadata operation]]
    (some->> resource-metadata
             :actions
             (some #(when (= (:name %) operation) %))
             :input-parameters
             (remove #(= (:type %) "map")))))


(reg-sub
  ::resource-metadata-attributes-options
  (fn [[_ resource-name]] (subscribe [::resource-metadata-attributes resource-name]))
  (fn [attributes]
    (map (fn [a] {:key a, :text a :value a}) (keys attributes))))
