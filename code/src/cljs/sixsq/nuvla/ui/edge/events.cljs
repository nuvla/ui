(ns sixsq.nuvla.ui.edge.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.edge.effects :as fx]
    [sixsq.nuvla.ui.edge.spec :as spec]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]))

(def refresh-id :nuvlabox-get-nuvlaboxes)

(reg-event-fx
  ::refresh
  (fn [_ _]
   {:dispatch [::main-events/action-interval-start {:id        refresh-id
                                                    :frequency 10000
                                                    :event     [::get-nuvlaboxes]}]}))


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::refresh]}))


(reg-event-fx
  ::set-full-text-search
  (fn [{{:keys [::spec/elements-per-page] :as db} :db} [_ full-text-search]]
    {:db       (assoc db ::spec/full-text-search full-text-search
                         ::spec/page 1)
     :dispatch [::refresh]}))


(reg-event-fx
  ::get-nuvlaboxes
  (fn [{{:keys [::spec/state-selector
                ::spec/page
                ::spec/elements-per-page
                ::spec/full-text-search] :as db} :db} _]
    {:db                   (assoc db ::spec/loading? true)
     ::cimi-api-fx/search  [:nuvlabox
                            (utils/get-query-params full-text-search page elements-per-page
                                                    state-selector)
                            #(dispatch [::set-nuvlaboxes %])]
     ::fx/state-nuvlaboxes [#(dispatch [::set-state-nuvlaboxes %])]}))


(reg-event-fx
  ::set-nuvlaboxes
  (fn [{:keys [db]} [_ {:keys [resources] :as nuvlaboxes}]]
    (if (instance? js/Error nuvlaboxes)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes)]
                   {:header  (cond-> (str "failure getting nuvlaboxes")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      (cond->
        {:db (assoc db ::spec/nuvlaboxes nuvlaboxes
                       ::spec/loading? false)}
        (not-empty resources) (assoc ::fx/get-status-nuvlaboxes
                                     [(map :id resources)
                                      #(dispatch [::set-status-nuvlaboxes %])])))))


(reg-event-db
  ::set-state-nuvlaboxes
  (fn [db [_ state-nuvlaboxes]]
    (assoc db ::spec/state-nuvlaboxes state-nuvlaboxes)))


(reg-event-db
  ::set-status-nuvlaboxes
  (fn [db [_ status-nuvlaboxes]]
    (assoc db ::spec/status-nuvlaboxes status-nuvlaboxes)))


(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    (dispatch [::get-nuvlaboxes])
    {:db (assoc db ::spec/state-selector state-selector
                   ::spec/page 1)}))


(reg-event-db
  ::open-modal
  (fn [db [_ modal-id]]
    (assoc db ::spec/open-modal modal-id)))


(reg-event-fx
  ::create-nuvlabox
  (fn [_ [_ creation-data]]
    {::cimi-api-fx/add [:nuvlabox creation-data
                        #(dispatch [::set-created-nuvlabox-id %])]}))


(reg-event-db
  ::set-created-nuvlabox-id
  (fn [db [_ {:keys [resource-id]}]]
    (dispatch [::get-nuvlaboxes])
    (assoc db ::spec/nuvlabox-created-id resource-id)))


(reg-event-db
  ::set-vpn-infra
  (fn [db [_ {:keys [resources]}]]
    (assoc db ::spec/vpn-infra resources)))


(reg-event-fx
  ::get-vpn-infra
  (fn [{:keys [db]} _]
    {:db                  (assoc db ::spec/vpn-infra nil)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter "subtype='vpn' and vpn-scope='nuvlabox'"
                            :select "id, name, description"
                            :last   10000}
                           #(dispatch [::set-vpn-infra %])]}))
