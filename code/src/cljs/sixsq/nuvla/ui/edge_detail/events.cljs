(ns sixsq.nuvla.ui.edge-detail.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.edge-detail.spec :as spec]
    [sixsq.nuvla.ui.history.events :as history-events]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-nuvlabox-status
  (fn [db [_ nuvlabox-status]]
    (assoc db ::spec/nuvlabox-status nuvlabox-status)))


(reg-event-fx
  ::set-nuvlabox
  (fn [{:keys [db]} [_ {nb-status-id :nuvlabox-status :as nuvlabox}]]
    {:db               (assoc db ::spec/nuvlabox nuvlabox
                                 ::spec/loading? false)
     ::cimi-api-fx/get [nb-status-id #(dispatch [::set-nuvlabox-status %])
                        :on-error #(dispatch [::set-nuvlabox-status nil])]}))


(reg-event-fx
  ::get-nuvlabox
  (fn [{{:keys [::spec/nuvlabox] :as db} :db} [_ id]]
    (cond-> {::cimi-api-fx/get [id #(dispatch [::set-nuvlabox %]) :on-error #(dispatch [::set-nuvlabox nil])]}
            (not= (:id nuvlabox) id) (assoc :db (merge db spec/defaults)))))


(reg-event-fx
  ::decommission
  (fn [{{:keys [::spec/nuvlabox]} :db} _]
    (let [nuvlabox-id (:id nuvlabox)]
      {::cimi-api-fx/operation [nuvlabox-id "decommission"
                                #(dispatch [::get-nuvlabox nuvlabox-id])]})))


(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/nuvlabox]} :db} _]
    (let [nuvlabox-id (:id nuvlabox)]
      {::cimi-api-fx/delete [nuvlabox-id #(dispatch [::history-events/navigate "edge"])]})))
