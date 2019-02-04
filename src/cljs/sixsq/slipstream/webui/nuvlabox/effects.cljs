(ns sixsq.slipstream.webui.nuvlabox.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.slipstream.webui.nuvlabox.utils :as u]))


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
      (let [[stale-count stale] (strip-health-info (<! (u/nuvlabox-search client u/stale-nb-machines)))
            [active-count active] (strip-health-info (<! (u/nuvlabox-search client u/active-nb-machines)))
            unhealthy (into {} (map #(vector % false) stale))
            healthy (into {} (map #(vector % true) active))
            healthy? (merge unhealthy healthy)]
        (callback {:stale-count  stale-count
                   :active-count active-count
                   :healthy?     healthy?})))))


(reg-fx
  ::get-nuvlabox-records
  (fn [_]
    (dispatch [:sixsq.slipstream.webui.nuvlabox.events/get-nuvlabox-records])))
