(ns sixsq.nuvla.ui.edge.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.cimi-api.effects :refer [CLIENT]]
    [sixsq.nuvla.ui.edge.utils :as utils]))


(reg-fx
  ::state-nuvlaboxes
  (fn [[callback]]
    (go
      (let [cimi-params  {:last        0
                          :aggregation "value_count:id,terms:state"}
            aggregations (:aggregations (<! (api/search @CLIENT :nuvlabox cimi-params)))
            states-count (->> aggregations
                              :terms:state
                              :buckets
                              (map (juxt :key :doc_count))
                              (into {}))]
        (callback {:total           (get-in aggregations [:value_count:id :value] 0)
                   :new             (get states-count utils/state-new 0)
                   :activated       (get states-count utils/state-activated 0)
                   :commissioned    (get states-count utils/state-commissioned 0)
                   :decommissioning (get states-count utils/state-decommissioning 0)
                   :decommissioned  (get states-count utils/state-decommissioned 0)
                   :error           (get states-count utils/state-error 0)})))))
