(ns sixsq.nuvla.ui.nuvlabox.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.main.effects :as main-fx]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.nuvlabox.effects :as fx]
    [sixsq.nuvla.ui.nuvlabox.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [sixsq.nuvla.ui.nuvlabox.utils :as utils]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-health-info
  (fn [db [_ state-info]]
    (assoc db
      ::spec/health-info state-info)))


(reg-event-fx
  ::fetch-health-info
  (fn [{{:keys [::client-spec/client]} :db} _]
    (when client
      {::fx/fetch-health-info [client #(dispatch [::set-health-info %])]})))


;; from CIMI


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    (dispatch [::get-nuvlaboxes])
    {:db (assoc db ::spec/page page)}))


(reg-event-fx
  ::get-nuvlaboxes
  (fn [{{:keys                            [::spec/state-selector
                                           ::spec/page
                                           ::spec/elements-per-page
                ::client-spec/client] :as db} :db} _]
    {:db                   (assoc db ::spec/loading? true)
     ::cimi-api-fx/search  [client
                            :nuvlabox
                            (general-utils/prepare-params
                              (cond-> {:first   (inc (* (dec page) elements-per-page))
                                       :last    (* page elements-per-page)
                                       :orderby "created:desc"
                                       :select  "id, name, state"}
                                      state-selector (assoc :filter (utils/state-filter state-selector))))
                            #(dispatch [::set-nuvlaboxes %])]
     ::fx/state-nuvlaboxes [client #(dispatch [::set-state-nuvlaboxes %])]}))


(reg-event-fx
  ::set-nuvlaboxes
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ {:keys [resources] :as nuvlaboxes}]]
    (if (instance? js/Error nuvlaboxes)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes)]
                   {:header  (cond-> (str "failure getting nuvlaboxes")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      {:db                        (assoc db ::spec/nuvlaboxes nuvlaboxes
                                            ::spec/loading? false)
       ::fx/get-status-nuvlaboxes [client resources #(dispatch [::set-status-nuvlaboxes %])]})))


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
