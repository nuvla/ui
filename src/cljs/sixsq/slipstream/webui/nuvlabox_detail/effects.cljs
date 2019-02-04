(ns sixsq.slipstream.webui.nuvlabox-detail.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [reg-fx]]
    [sixsq.slipstream.client.api.cimi :as cimi]))


(reg-fx
  ::fetch-detail
  (fn [[client mac callback]]
    (go
      (when (and mac callback)
        (let [state (<! (cimi/get client (str "nuvlabox-state/" mac)))
              record (<! (cimi/get client (str "nuvlabox-record/" mac)))]
          (callback state record))))))
