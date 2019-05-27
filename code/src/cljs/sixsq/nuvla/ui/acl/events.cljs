(ns sixsq.nuvla.ui.acl.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.acl.spec :as spec]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-principal
  (fn [db [_ id principal-name]]
    (when id
      (assoc-in db [::spec/users-and-groups id] principal-name))))


(reg-event-fx
  ::get-principal
  (fn [{{:keys [::client-spec/client] :as db} :db :as cofx} [_ principal]]
    (when principal
      {::cimi-api-fx/get [client principal #(dispatch [::set-principal principal (:name %)])]})))


(reg-event-db
  ::set-users-options
  (fn [db [_ {:keys [resources]}]]
    (when resources
      (assoc db ::spec/users-options resources))))



(reg-event-fx
  ::search-users
  (fn [{{:keys [::client-spec/client] :as db} :db :as cofx} [_ users-search]]
    {::cimi-api-fx/search [client :user {:filter (str "fulltext=='" users-search "*'")
                                         :select "id, name"
                                         :order  "name:asc, id:asc"
                                         :last   10} #(dispatch [::set-users-options %])]}))


(reg-event-db
  ::set-groups-options
  (fn [db [_ {:keys [resources]}]]
    (when resources
      (assoc db ::spec/groups-options resources))))



(reg-event-fx
  ::search-groups
  (fn [{{:keys [::client-spec/client] :as db} :db :as cofx} _]
    {::cimi-api-fx/search [client :group {:select "id, name"
                                          :order  "name:asc, id:asc"} #(dispatch [::set-groups-options %])]}))