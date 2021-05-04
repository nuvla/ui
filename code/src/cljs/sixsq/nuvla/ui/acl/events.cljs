(ns sixsq.nuvla.ui.acl.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.acl.spec :as spec]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-event-db
  ::set-principal
  (fn [db [_ id principal-name]]
    (when id
      (assoc-in db [::spec/users-and-groups id] principal-name))))


;; Swallow the error, since acl can contain groups for which the user has no access.
;; This will leave the group in the acl as the id instead of its name.
(reg-event-fx
  ::get-principal
  (fn [_ [_ principal]]
    (when principal
      {::cimi-api-fx/get [principal
                          #(dispatch [::set-principal principal (:name %)])
                          :on-error #()]})))


(reg-event-db
  ::set-users-options
  (fn [db [_ {:keys [resources]}]]
    (when resources
      (assoc db ::spec/users-options resources))))


(reg-event-fx
  ::search-users
  (fn [_ [_ users-search]]
    (let [filter-str (general-utils/fulltext-query-string "name" users-search)]
      {::cimi-api-fx/search [:user {:filter filter-str
                                    :select "id, name"
                                    :order  "name:asc, id:asc"
                                    :last   10} #(dispatch [::set-users-options %])]})))


(reg-event-db
  ::set-groups-options
  (fn [db [_ {:keys [resources]}]]
    (when resources
      (assoc db ::spec/groups-options resources))))


(reg-event-fx
  ::search-groups
  (fn [_ _]
    {::cimi-api-fx/search [:group {:select "id, name"
                                   :order  "name:asc, id:asc"}
                           #(dispatch [::set-groups-options %])]}))