(ns sixsq.nuvla.ui.nuvlabox-detail.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [reg-fx]]
    [sixsq.nuvla.client.api :as api]))


(reg-fx
  ::fetch-detail
  (fn [[client mac callback]]
    (go
      (when (and mac callback)
        (let [state (<! (api/get client (str "nuvlabox-state/" mac)))
              record (<! (api/get client (str "nuvlabox-record/" mac)))]
          (callback state record))))))
