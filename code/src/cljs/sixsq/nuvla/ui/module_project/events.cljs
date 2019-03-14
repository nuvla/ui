(ns sixsq.nuvla.ui.module-project.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.module-project.spec :as spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::name
  (fn [db [_ name]]
    (dispatch [::page-changed? true])
    (assoc db ::spec/name name)))


(reg-event-db
  ::parent
  (fn [db [_ parent]]
    (dispatch [::page-changed? true])
    (assoc db ::spec/parent parent)))


(reg-event-db
  ::description
  (fn [db [_ description]]
    (dispatch [::page-changed? true])
    (assoc db ::spec/description description)))


(reg-event-db
  ::save-logo-url
  (fn [db [_ logo-url]]
    (dispatch [::page-changed? true])
    (assoc db ::spec/logo-url logo-url
              ::spec/logo-url-modal-visible? false)))


(reg-event-db
  ::open-logo-url-modal
  (fn [db _]
    (assoc db ::spec/logo-url-modal-visible? true)))


(reg-event-db
  ::close-logo-url-modal
  (fn [db _]
    (assoc db ::spec/logo-url-modal-visible? false)))


(reg-event-db
  ::page-changed?
  (fn [db [_ has-change?]]
    (log/infof "dirty %s" has-change?)
    (assoc db ::spec/page-changed? has-change?)))
