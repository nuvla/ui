(ns sixsq.nuvla.ui.data.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.cimi-api.effects :refer [CLIENT]]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-fx
  ::fetch-data
  (fn [[time-period-filter full-text-search data-sets callback]]
    (go
      (doseq [{:keys [id] :as data-set} data-sets]
        (let [record-filter (:data-record-filter data-set)
              filter        (general-utils/join-and
                              time-period-filter full-text-search record-filter)]
          (callback id (<! (api/search @CLIENT
                                       :data-record
                                       {:filter      filter
                                        :select      "id"
                                        :aggregation "value_count:id, sum:bytes"}))))))))
