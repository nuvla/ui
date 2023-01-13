(ns sixsq.nuvla.ui.clouds-detail.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.clouds-detail.spec :as spec]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-db ::set-infrastructure-service
              (fn [db [_ infrastructure-service]]
                (assoc db ::spec/infra-service-not-found? (nil? infrastructure-service)
                          ::spec/infrastructure-service infrastructure-service
                          ::main-spec/loading? false)))

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

(def on-success #(dispatch [::routing-events/navigate (name->href routes/clouds)]))

(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/infrastructure-service]} :db} _]
    (let [infra-id (:id infrastructure-service)]
      {::cimi-api-fx/delete [infra-id on-success]})))

(reg-event-fx
  ::operation
  (fn [{{:keys [::spec/infrastructure-service]} :db} [_ operation]]
    (let [infra-id (:id infrastructure-service)]
      {::cimi-api-fx/operation [infra-id operation on-success]})))
