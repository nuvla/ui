(ns sixsq.nuvla.ui.edge.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.cimi-api.effects :refer [CLIENT]]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [taoensso.timbre :as log]))


(defn get-state-count
  [state]
  (api/search @CLIENT :nuvlabox (cond-> {:last 0}
                                        state (assoc :filter (utils/state-filter state)))))


(reg-fx
  ::state-nuvlaboxes
  (fn [[callback]]
    (go
      (let [total           (<! (get-state-count nil))
            new             (<! (get-state-count utils/state-new))
            activated       (<! (get-state-count utils/state-activated))
            commissioned    (<! (get-state-count utils/state-commissioned))
            decommissioning (<! (get-state-count utils/state-decommissioning))
            decommissioned  (<! (get-state-count utils/state-decommissioned))
            error           (<! (get-state-count utils/state-error))]

        (callback {:total           (:count total)
                   :new             (:count new)
                   :activated       (:count activated)
                   :commissioned    (:count commissioned)
                   :decommissioning (:count decommissioning)
                   :decommissioned  (:count decommissioned)
                   :error           (:count error)})))))



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
      (let [offline-nuvlaboxes      (<! (get-status-collection
                                          nuvlaboxes-ids
                                          utils/filter-offline-status))
            online-nuvlaboxes       (<! (get-status-collection
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
