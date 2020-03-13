(ns sixsq.nuvla.ui.edge-detail.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.edge-detail.spec :as spec]
    [sixsq.nuvla.ui.edge.effects :as edge-fx]
    [sixsq.nuvla.ui.edge.events :as edge-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-db
  ::set-nuvlabox-status
  (fn [db [_ nuvlabox-status]]
    (assoc db ::spec/nuvlabox-status nuvlabox-status)))


(reg-event-db
  ::set-nuvlabox-peripherals
  (fn [db [_ nuvlabox-peripherals]]
    (assoc db ::spec/nuvlabox-peripherals (->> (get nuvlabox-peripherals :resources [])
                                               (map (juxt :id identity))
                                               (into {})))))


(reg-event-fx
  ::set-nuvlabox
  (fn [{:keys [db]} [_ {nb-status-id :nuvlabox-status id :id :as nuvlabox}]]
    {:db                             (assoc db ::spec/nuvlabox nuvlabox
                                               ::spec/loading? false)
     ::cimi-api-fx/get               [nb-status-id #(dispatch [::set-nuvlabox-status %])
                                      :on-error #(dispatch [::set-nuvlabox-status nil])]
     ::edge-fx/get-status-nuvlaboxes [[id] #(dispatch [::edge-events/set-status-nuvlaboxes %])]}))


(reg-event-fx
  ::get-nuvlabox
  (fn [{{:keys [::spec/nuvlabox] :as db} :db} [_ id]]
    (cond-> {::cimi-api-fx/get    [id #(dispatch [::set-nuvlabox %])
                                   :on-error #(dispatch [::set-nuvlabox nil])]
             ::cimi-api-fx/search [:nuvlabox-peripheral
                                   {:filter  (str "parent='" id "'")
                                    :last    10000
                                    :orderby "id"}
                                   #(dispatch [::set-nuvlabox-peripherals %])]}
            (not= (:id nuvlabox) id) (assoc :db (merge db spec/defaults)))))


(reg-event-fx
  ::decommission
  (fn [{{:keys [::spec/nuvlabox]} :db} _]
    (let [nuvlabox-id (:id nuvlabox)]
      {::cimi-api-fx/operation [nuvlabox-id "decommission"
                                #(dispatch [::get-nuvlabox nuvlabox-id])]})))

(reg-event-fx
  ::edit
  (fn [_ [_ resource-id data success-msg]]
    {::cimi-api-fx/edit [resource-id data
                         #(if (instance? js/Error %)
                            (let [{:keys [status message]} (response/parse-ex-info %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "error editing " resource-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :error}]))
                            (do
                              (when success-msg
                                (dispatch [::messages-events/add
                                           {:header  success-msg
                                            :content success-msg
                                            :type    :success}]))
                              (dispatch [::set-nuvlabox %])))]}))


(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/nuvlabox]} :db} _]
    (let [nuvlabox-id (:id nuvlabox)]
      {::cimi-api-fx/delete [nuvlabox-id #(dispatch [::history-events/navigate "edge"])]})))


(reg-event-fx
  ::custom-action
  (fn [_ [_ resource-id operation success-msg]]
    {::cimi-api-fx/operation [resource-id operation
                              #(if (instance? js/Error %)
                                 (let [{:keys [status message]} (response/parse-ex-info %)]
                                   (dispatch [::messages-events/add
                                              {:header  (cond-> (str "error on operation " operation " for " resource-id)
                                                                status (str " (" status ")"))
                                               :content message
                                               :type    :error}]))

                                 (when success-msg
                                   (dispatch [::messages-events/add
                                              {:header  success-msg
                                               :content success-msg
                                               :type    :success}])))]}))
