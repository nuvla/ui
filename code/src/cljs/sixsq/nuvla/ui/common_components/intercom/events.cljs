(ns sixsq.nuvla.ui.main.intercom.events
  (:require [re-frame.core :refer [reg-event-db]]
            [sixsq.nuvla.ui.main.intercom.spec :as spec]))


(reg-event-db
  ::set-event
  (fn [{:keys [::spec/events] :as db} [_ event-name event-value]]
    (assoc-in db [::spec/events] (merge events {event-name event-value}))))


(reg-event-db
  ::clear-events
  (fn [db _]
    (assoc-in db [::spec/events] {})))
