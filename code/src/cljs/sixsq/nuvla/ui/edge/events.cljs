(ns sixsq.nuvla.ui.edge.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.edge.effects :as fx]
    [sixsq.nuvla.ui.edge.spec :as spec]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


;; from CIMI


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    (dispatch [::get-nuvlaboxes])
    {:db (assoc db ::spec/page page)}))


(reg-event-fx
  ::get-nuvlaboxes
  (fn [{{:keys [::spec/state-selector
                ::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    {:db                   (assoc db ::spec/loading? true)
     ::cimi-api-fx/search  [:nuvlabox
                            (general-utils/prepare-params
                              (cond-> {:first   (inc (* (dec page) elements-per-page))
                                       :last    (* page elements-per-page)
                                       :orderby "created:desc"}
                                      state-selector (assoc :filter (utils/state-filter state-selector))))
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
                                     [resources #(dispatch [::set-status-nuvlaboxes %])])))))


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
  (fn [_ [_ owner-id]]
    {::cimi-api-fx/add [:nuvlabox {:owner            owner-id
                                   :refresh-interval 30}
                        #(dispatch [::set-created-nuvlabox-id %])]}))


(reg-event-db
  ::set-created-nuvlabox-id
  (fn [db [_ {:keys [resource-id]}]]
    (dispatch [::get-nuvlaboxes])
    (assoc db ::spec/nuvlabox-created-id resource-id)))