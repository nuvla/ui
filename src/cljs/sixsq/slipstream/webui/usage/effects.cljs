(ns sixsq.slipstream.webui.usage.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.slipstream.webui.usage.utils :as usage-utils]))


(reg-fx
  ::fetch-totals
  (fn [[client
        date-after
        date-before
        credentials
        billable-only?
        callback]]
    (go
      (callback (<! (usage-utils/fetch-totals client
                                              date-after
                                              date-before
                                              credentials
                                              billable-only?))))))


(reg-fx
  ::fetch-meterings
  (fn [[client
        date-after
        date-before
        credentials
        billable-only?
        callback]]
    (usage-utils/fetch-meterings client
                                 date-after
                                 date-before
                                 credentials
                                 billable-only?
                                 callback)))
