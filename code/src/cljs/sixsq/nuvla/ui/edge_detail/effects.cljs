(ns sixsq.nuvla.ui.edge-detail.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.cimi-api.effects :refer [CLIENT]]))


(reg-fx
  ::fetch-detail
  (fn [[mac callback]]
    (go
      (when (and mac callback)
        (let [state  (<! (api/get @CLIENT (str "nuvlabox-state/" mac)))
              record (<! (api/get @CLIENT (str "nuvlabox-record/" mac)))]
          (callback state record))))))
