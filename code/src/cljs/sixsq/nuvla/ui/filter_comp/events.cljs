(ns sixsq.nuvla.ui.filter-comp.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.routing.utils :refer [get-query-param]]))


(reg-event-fx
  ::terms-attribute
  (fn [_ [_ resource-type attribute values-atom]]
    (let [agg-term (str "terms:" attribute)]
      {::cimi-api-fx/search [resource-type {:aggregation (str "terms:" attribute)
                                            :last        0}
                             #(reset! values-atom
                                      (-> %
                                          :aggregations
                                          (get (keyword agg-term))
                                          :buckets
                                          (->> (map :key))))]})))

(reg-event-fx
 ::init-filter
 (fn [{db :db} [_ {:keys [on-done resource-name]}]]
   (let [filter-query (get-query-param (:current-route db) (keyword resource-name))]
     (when (seq filter-query) (on-done filter-query)))))