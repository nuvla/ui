(ns sixsq.nuvla.ui.module-component.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.module-component.spec :as spec]
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
  ::add-port-mapping
  (fn [db [_ id mapping]]
    (dispatch [::page-changed? true])
    ; overwrite the id
    (assoc-in db [::spec/port-mappings id] (assoc mapping :id id))))


(reg-event-db
  ::remove-port-mapping
  (fn [db [_ id]]
    (dispatch [::page-changed? true])
    (update-in db [::spec/port-mappings] dissoc id)))


(reg-event-db
  ::update-mapping-source
  (fn [db [_ id value]]
    (dispatch [::page-changed? true])
    (assoc-in db [::spec/port-mappings id :source] value)))


(reg-event-db
  ::update-mapping-destination
  (fn [db [_ id value]]
    (dispatch [::page-changed? true])
    (assoc-in db [::spec/port-mappings id :destination] value)))


(reg-event-db
  ::update-mapping-port-type
  (fn [db [_ id value]]
    (assoc-in db [::spec/port-mappings id :port-type] value)))


(reg-event-db
  ::add-volume
  (fn [db [_ id volume]]
    (dispatch [::page-changed? true])
    ; overwrite the id
    (assoc-in db [::spec/volumes id] (assoc volume :id id))))


(reg-event-db
  ::remove-volume
  (fn [db [_ id]]
    (dispatch [::page-changed? true])
    (update-in db [::spec/volumes] dissoc id)))


(reg-event-db
  ::update-volume-type
  (fn [db [_ id value]]
    (dispatch [::page-changed? true])
    (assoc-in db [::spec/volumes id :type] value)))


(reg-event-db
  ::update-volume-source
  (fn [db [_ id value]]
    (dispatch [::page-changed? true])
    (assoc-in db [::spec/volumes id :source] value)))


(reg-event-db
  ::update-volume-destination
  (fn [db [_ id value]]
    (dispatch [::page-changed? true])
    (assoc-in db [::spec/volumes id :destination] value)))


(reg-event-db
  ::update-volume-driver
  (fn [db [_ id value]]
    (assoc-in db [::spec/volumes id :driver] value)))


(reg-event-db
  ::update-volume-options
  (fn [db [_ id value]]
    (assoc-in db [::spec/volumes id :options] value)))


(reg-event-db
  ::page-changed?
  (fn [db [_ has-change?]]
    (assoc db ::spec/page-changed? has-change?)))


(reg-event-db
  ::commit-message
  (fn [db [_ msg]]
    (assoc db ::spec/commit-message msg)))


(reg-event-db
  ::open-save-modal
  (fn [db _]
    (assoc db ::spec/save-modal-visible? true)))


(reg-event-db
  ::close-save-modal
  (fn [db _]
    (assoc db ::spec/save-modal-visible? false)))


;(reg-event-fx
;  ::save-module
;  (fn [_] {}))
