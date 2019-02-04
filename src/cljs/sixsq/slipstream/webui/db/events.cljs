(ns sixsq.slipstream.webui.db.events
  (:require
    [re-frame.core :refer [reg-event-db]]
    [sixsq.slipstream.webui.db.spec :as db]))


(reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))
