(ns sixsq.nuvla.ui.filter-comp.subs
  (:require
    [re-frame.core :refer [subscribe dispatch reg-sub]]
    [sixsq.nuvla.ui.filter-comp.spec :as spec]))


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
  (fn [resource-metadta]
    (->> (loop [attrs  (:attributes resource-metadta)
                result []]
           (if (seq attrs)
             (let [{:keys [leafs nested]} (group-by #(if (empty? (:child-types %))
                                                       :leafs :nested) attrs)
                   new-attrs (mapcat (fn [{:keys [type child-types] :as in}]
                                       (if (= type "array")
                                         [(-> in
                                              (dissoc :child-types)
                                              (assoc :type (-> child-types first :type)))]
                                         (map #(assoc % :name (str (:name in) "/" (:name %)))
                                              child-types))) nested)]
               (recur new-attrs (concat result leafs)))
             result))
         (filter :indexed)
         (map (juxt :name identity))
         (into (sorted-map)))))


(reg-sub
  ::resource-metadata-attributes-options
  (fn [[_ resource-name]] (subscribe [::resource-metadata-attributes resource-name]))
  (fn [attributes]
    (map (fn [a] {:key a, :text a :value a}) (keys attributes))))

