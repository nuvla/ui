(ns sixsq.nuvla.ui.data.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.data.utils :as utils]))


(reg-fx
  ::fetch-data
  (fn [[client time-period-filter cloud-filter full-text-search datasets callback]]
    (go
      (when client
        (doseq [{:keys [id] :as dataset} datasets]
          (let [objectFilter (get dataset (keyword "dataset:objectFilter"))
                filter (utils/join-and time-period-filter cloud-filter full-text-search objectFilter)]
            (callback id (<! (api/search client
                                         "serviceOffers"
                                         {:filter      filter
                                          :select      "id"
                                          :aggregation "count:id, sum:data:bytes"})))))))))
