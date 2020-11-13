(ns sixsq.nuvla.ui.filter-comp.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.filter-comp.spec :as spec]
    [sixsq.nuvla.ui.filter-comp.utils :as utils]))


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
  ::resource-metadata-attributes-options
  (fn [[_ resource-name]] (subscribe [::resource-metadata-attributes resource-name]))
  (fn [attributes]
    (map (fn [a] {:key a, :text a :value a}) (keys attributes))))

