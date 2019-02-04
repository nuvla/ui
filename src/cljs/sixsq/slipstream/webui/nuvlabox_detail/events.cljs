(ns sixsq.slipstream.webui.nuvlabox-detail.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.nuvlabox-detail.effects :as nuvlabox-fx]
    [sixsq.slipstream.webui.nuvlabox-detail.spec :as nuvlabox-spec]))


(reg-event-db
  ::set-mac
  (fn [db [_ mac]]
    (if mac
      (assoc db ::nuvlabox-spec/mac mac)
      (assoc db ::nuvlabox-spec/mac mac
                ::nuvlabox-spec/state nil
                ::nuvlabox-spec/record nil))))


(reg-event-db
  ::set-detail
  (fn [db [_ state record]]
    (assoc db
      ::nuvlabox-spec/loading? false
      ::nuvlabox-spec/state state
      ::nuvlabox-spec/record record)))


(reg-event-db
  ::clear-detail
  (fn [db _]
    (assoc db
      ::nuvlabox-spec/loading? false
      ::nuvlabox-spec/state nil
      ::nuvlabox-spec/record nil)))


(reg-event-fx
  ::fetch-detail
  (fn [{{:keys [::nuvlabox-spec/mac] :as db} :db} _]
    (if-let [client (::client-spec/client db)]
      {:db                        (assoc db ::nuvlabox-spec/loading? true)
       ::nuvlabox-fx/fetch-detail [client mac
                                   (fn [state record]
                                     (dispatch [::set-detail state record]))]}
      {:db db})))
