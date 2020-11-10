(ns sixsq.nuvla.ui.filter-comp.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.filter-comp.spec :as spec]
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


(reg-event-db
  ::set-resource-metadata
  (fn [db [_ resource-name metadata]]
    (assoc-in db [::spec/resource-metadata resource-name] metadata)))


(reg-event-fx
  ::get-resource-metadata
  (fn [{{:keys [::spec/resource-metadata]} :db} [_ resource-name]]
    (when (nil? (get resource-metadata resource-name))
      {::cimi-api-fx/get [(str "resource-metadata/"
                               (case resource-name
                                 "nuvlabox" "nuvlabox-1"
                                 "nuvlabox-status" "nuvlabox-status-1"
                                 "nuvlabox-peripheral" "nuvlabox-peripheral-1-1"
                                 resource-name))
                          #(dispatch [::set-resource-metadata resource-name %])]})))
