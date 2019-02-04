(ns sixsq.slipstream.webui.nuvlabox.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.main.effects :as main-fx]
    [sixsq.slipstream.webui.messages.events :as messages-events]
    [sixsq.slipstream.webui.nuvlabox.effects :as nuvlabox-fx]
    [sixsq.slipstream.webui.nuvlabox.spec :as nuvlabox-spec]
    [sixsq.slipstream.webui.utils.general :as general-utils]
    [sixsq.slipstream.webui.utils.response :as response]))


(reg-event-db
  ::set-health-info
  (fn [db [_ state-info]]
    (assoc db
      ::nuvlabox-spec/health-info state-info)))


(reg-event-fx
  ::fetch-health-info
  (fn [{:keys [db]} _]
    (if-let [client (::client-spec/client db)]
      {::nuvlabox-fx/fetch-health-info [client #(dispatch [::set-health-info %])]}
      {:db db})))


;; from CIMI


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    {:db                                (assoc db ::nuvlabox-spec/page page)
     ::nuvlabox-fx/get-nuvlabox-records nil}))


(reg-event-fx
  ::get-nuvlabox-records
  (fn [{{:keys [::nuvlabox-spec/state-selector
                ::nuvlabox-spec/page
                ::nuvlabox-spec/elements-per-page
                ::client-spec/client] :as db} :db} _]
    (let [resource-type :nuvlaboxRecords
          filter (case state-selector
                   "new" "state='new'"
                   "activated" "state='activated'"
                   "quarantined" "state='quarantined'"
                   nil)]
      {:db                  (assoc db ::nuvlabox-spec/loading? true
                                      ::nuvlabox-spec/nuvlabox-records nil)
       ::cimi-api-fx/search [client
                             resource-type
                             (general-utils/prepare-params {:$filter  filter
                                                            :$first   (inc (* (dec page) elements-per-page))
                                                            :$last    (* page elements-per-page)
                                                            :$orderby "created:desc"
                                                            :$select  "id, macAddress, state, name"})
                             #(dispatch [::set-nuvlabox-records resource-type %])]})))


(reg-event-fx
  ::set-nuvlabox-records
  (fn [{db :db} [_ resource-type listing]]
    (let [error? (instance? js/Error listing)]
      (when error?
        (dispatch [::messages-events/add
                   (let [{:keys [status message]} (response/parse-ex-info listing)]
                     {:header  (cond-> (str "failure getting " (name resource-type))
                                       status (str " (" status ")"))
                      :content message
                      :type    :error})]))
      {:db                       (assoc db ::nuvlabox-spec/nuvlabox-records (when-not error? listing)
                                           ::nuvlabox-spec/loading? false)
       ::main-fx/action-interval [{:action    :start
                                   :id        :nuvlabox-health-info
                                   :frequency 30000
                                   :event     [::fetch-health-info]}]})))


(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    {:db                                (assoc db
                                          ::nuvlabox-spec/state-selector state-selector
                                          ::nuvlabox-spec/page 1)
     ::nuvlabox-fx/get-nuvlabox-records nil}))
