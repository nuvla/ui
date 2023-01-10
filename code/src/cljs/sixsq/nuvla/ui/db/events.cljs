(ns sixsq.nuvla.ui.db.events
  (:require [re-frame.core :refer [reg-event-db]]
            [sixsq.nuvla.ui.db.spec :as db]))


(reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))
