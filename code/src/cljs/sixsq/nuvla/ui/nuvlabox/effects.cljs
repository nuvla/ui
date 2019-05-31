(ns sixsq.nuvla.ui.nuvlabox.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.nuvlabox.utils :as u]
    [sixsq.nuvla.ui.nuvlabox.utils :as utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [taoensso.timbre :as log]))


(defn strip-health-info
  [{:keys [count] :as states-collection}]
  [count (->> states-collection
              :nuvlaboxStates
              (map :nuvlabox)
              (map :href))])


(reg-fx
  ::fetch-health-info
  (fn [[client callback]]
    (go
      (let [[stale-count stale] (strip-health-info (<! (u/nuvlabox-status-search client u/stale-nb-machines)))
            [active-count active] (strip-health-info (<! (u/nuvlabox-status-search client u/active-nb-machines)))
            unhealthy (into {} (map #(vector % false) stale))
            healthy   (into {} (map #(vector % true) active))
            healthy?  (merge unhealthy healthy)]
        (callback {:stale-count  stale-count
                   :active-count active-count
                   :healthy?     healthy?})))))


(defn get-state-count
  [client state]
  (api/search client :nuvlabox {:filter (utils/state-filter state)
                                :last   0}))


(reg-fx
  ::state-nuvlaboxes
  (fn [[client callback]]
    (go
      (let [new             (<! (get-state-count client utils/state-new))
            activated       (<! (get-state-count client utils/state-activated))
            quarantined     (<! (get-state-count client utils/state-quarantined))
            decommissioning (<! (get-state-count client utils/state-decommissioning))
            error           (<! (get-state-count client utils/state-error))]

        (callback {:new             (:count new)
                   :activated       (:count activated)
                   :quarantined     (:count quarantined)
                   :decommissioning (:count decommissioning)
                   :error           (:count error)})))))



(defn get-status-collection
  [client nuvlaboxes filter-heartbeat]
  (let [filter-nuvlabox-ids (->> nuvlaboxes
                                 (map #(str "parent='" (:id %) "'"))
                                 (apply general-utils/join-or))
        filter              (general-utils/join-and filter-nuvlabox-ids filter-heartbeat)]

    (api/search client :nuvlabox-status {:filter filter
                                         :select "id, parent"})))


(reg-fx
  ::get-status-nuvlaboxes
  (fn [[client nuvlaboxes callback]]
    (go
      (let [floating-time-tolerance "-10s"
            offline-nuvlaboxes      (<! (get-status-collection
                                          client nuvlaboxes
                                          (str "next-heartbeat < 'now" floating-time-tolerance "'")))
            online-nuvlaboxes       (<! (get-status-collection
                                          client nuvlaboxes
                                          (str "next-heartbeat >= 'now" floating-time-tolerance "'")))]

        (callback {:offline (->> offline-nuvlaboxes
                                 :resources
                                 (map :parent)
                                 (set))
                   :online  (->> online-nuvlaboxes
                                 :resources
                                 (map :parent)
                                 (set))})))))
