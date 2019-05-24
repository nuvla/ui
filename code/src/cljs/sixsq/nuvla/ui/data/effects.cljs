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
  (fn [[client time-period-filter infra-services-filter full-text-search data-sets callback]]
    (go
      (when client
        (doseq [{:keys [id] :as data-set} data-sets]
          (let [record-filter (:data-record-filter data-set)
                filter        (utils/join-and time-period-filter infra-services-filter full-text-search record-filter)]
            (callback id (<! (api/search client
                                         :data-record
                                         {:filter      filter
                                          :select      "id"
                                          :aggregation "value_count:id, sum:bytes"})))))))))
