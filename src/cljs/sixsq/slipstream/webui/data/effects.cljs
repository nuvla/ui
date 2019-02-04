(ns sixsq.slipstream.webui.data.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [sixsq.slipstream.webui.data.utils :as utils]))


(reg-fx
  ::fetch-data
  (fn [[client time-period-filter cloud-filter full-text-search datasets callback]]
    (go
      (when client
        (doseq [{:keys [id] :as dataset} datasets]
          (let [objectFilter (get dataset (keyword "dataset:objectFilter"))
                filter (utils/join-and time-period-filter cloud-filter full-text-search objectFilter)]
            (callback id (<! (cimi/search client
                                          "serviceOffers"
                                          {:$filter      filter
                                           :$select      "id"
                                           :$aggregation "count:id, sum:data:bytes"})))))))))
