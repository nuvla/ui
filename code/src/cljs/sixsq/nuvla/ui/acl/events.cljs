(ns sixsq.nuvla.ui.acl.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.acl.spec :as spec]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]))


(reg-event-db
  ::set-groups
  (fn [db [_ {:keys [resources]}]]
    (when resources
      (assoc db ::spec/groups resources))))


(reg-event-fx
  ::search-groups
  (fn [_ _]
    {::cimi-api-fx/search [:group {:select "id, name, acl, users, description"
                                   :order  "name:asc, id:asc"}
                           #(dispatch [::set-groups %])]}))
