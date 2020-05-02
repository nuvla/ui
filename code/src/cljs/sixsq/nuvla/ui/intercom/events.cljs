(ns sixsq.nuvla.ui.intercom.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx subscribe]]
    [sixsq.nuvla.ui.intercom.spec :as spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-event
  (fn [{:keys [::spec/events] :as db} [_ event-name event-value]]
    (assoc-in db [::spec/events] (merge events {event-name event-value}))))
