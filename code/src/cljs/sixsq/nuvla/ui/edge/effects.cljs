(ns sixsq.nuvla.ui.edge.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.cimi-api.effects :refer [CLIENT]]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


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


(defn get-status-collection
  [nuvlabox-ids filter-heartbeat]
  (let [filter-nuvlabox-ids (->> nuvlabox-ids
                                 (map #(str "parent='" % "'"))
                                 (apply general-utils/join-or))
        filter              (general-utils/join-and filter-nuvlabox-ids filter-heartbeat)]

    (api/search @CLIENT :nuvlabox-status {:filter filter
                                          :select "id, parent, updated, next-heartbeat"})))


(reg-fx
  ::get-status-nuvlaboxes
  (fn [[nuvlaboxes-ids callback]]
    (go
      (let [offline-nuvlaboxes (<! (get-status-collection
                                     nuvlaboxes-ids
                                     utils/filter-offline-status))
            online-nuvlaboxes  (<! (get-status-collection
                                     nuvlaboxes-ids
                                     utils/filter-online-status))]

        (callback {:offline (->> offline-nuvlaboxes
                                 :resources
                                 (map :parent)
                                 (set))
                   :online  (->> online-nuvlaboxes
                                 :resources
                                 (map :parent)
                                 (set))})))))
