(ns sixsq.nuvla.ui.edge-detail.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.edge-detail.spec :as spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-nuvlabox-status
  (fn [db [_ nuvlabox-status]]
    (assoc db ::spec/nuvlabox-status nuvlabox-status)))


(reg-event-fx
  ::set-nuvlabox
  (fn [{:keys [db]} [_ {nuvlabox-status-id :nuvlabox-status :as nuvlabox}]]
    (cond-> {:db (assoc db ::spec/nuvlabox nuvlabox)}
            nuvlabox-status-id (assoc ::cimi-api-fx/get
                                      [nuvlabox-status-id
                                       #(dispatch [::set-nuvlabox-status %])]))))


(reg-event-fx
  ::get-nuvlabox
  (fn [{{:keys [::spec/nuvlabox] :as db} :db} [_ id]]
    (cond-> {::cimi-api-fx/get [id #(dispatch [::set-nuvlabox %])]}
            (not= (:id nuvlabox) id) (assoc :db (merge db spec/defaults)))))

