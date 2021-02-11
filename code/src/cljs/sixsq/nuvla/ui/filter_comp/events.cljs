(ns sixsq.nuvla.ui.filter-comp.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]))


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
