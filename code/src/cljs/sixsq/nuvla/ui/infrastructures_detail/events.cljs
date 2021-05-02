(ns sixsq.nuvla.ui.infrastructures-detail.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.infrastructures-detail.spec :as spec]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-db
  ::set-infrastructure-service
  (fn [db [_ infrastructure-service]]
    (assoc db ::spec/infrastructure-service infrastructure-service
              ::spec/loading? false)))


(reg-event-fx
  ::get-infrastructure-service
  (fn [{{:keys [::spec/infrastructure-service] :as db} :db} [_ id]]
    (cond-> {::cimi-api-fx/get [id #(dispatch [::set-infrastructure-service %])
                                :on-error #(dispatch [::set-infrastructure-service nil])]}
            (not= (:id infrastructure-service) id) (assoc :db (merge db spec/defaults)))))


(reg-event-fx
  ::edit-infrastructure-service
  (fn [{{:keys [::spec/infrastructure-service]} :db} _]
    (let [resource-id (:id infrastructure-service)]
      {::cimi-api-fx/edit [resource-id infrastructure-service
                           #(if (instance? js/Error %)
                              (let [{:keys [status message]} (response/parse-ex-info %)]
                                (dispatch [::messages-events/add
                                           {:header  (cond-> (str "error editing " resource-id)
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :error}]))
                              (do
                                (dispatch [::main-events/changes-protection? false])
                                (dispatch [::set-infrastructure-service %])))]})))


(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/infrastructure-service]} :db} _]
    (let [infra-id (:id infrastructure-service)]
      {::cimi-api-fx/delete [infra-id #(dispatch
                                         [::history-events/navigate "infrastructures"])]})))


(reg-event-fx
  ::terminate
  (fn [{{:keys [::spec/infrastructure-service]} :db} _]
    (let [infra-id (:id infrastructure-service)]
      {::cimi-api-fx/operation [infra-id "terminate" #(dispatch [::history-events/navigate "infrastructures"])]})))


(reg-event-fx
  ::stop
  (fn [{{:keys [::spec/infrastructure-service]} :db} _]
    (let [infra-id (:id infrastructure-service)]
      {::cimi-api-fx/operation [infra-id "stop" #(dispatch [::history-events/navigate "infrastructures"])]})))


(reg-event-fx
  ::start
  (fn [{{:keys [::spec/infrastructure-service]} :db} _]
    (let [infra-id (:id infrastructure-service)]
      {::cimi-api-fx/operation [infra-id "start" #(dispatch [::history-events/navigate "infrastructures"])]})))
