(ns sixsq.nuvla.ui.apps-component.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-component.spec :as spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::add-port-mapping
  (fn [db [_ id mapping]]
    ; overwrite the id
    (assoc-in db [::spec/port-mappings id] (assoc mapping :id id))))


(reg-event-db
  ::remove-port-mapping
  (fn [db [_ id]]
    (update-in db [::spec/port-mappings] dissoc id)))


(reg-event-db
  ::update-mapping-source
  (fn [db [_ id value]]
    (assoc-in db [::spec/port-mappings id :source] value)))


(reg-event-db
  ::update-mapping-destination
  (fn [db [_ id value]]
    (assoc-in db [::spec/port-mappings id :destination] value)))


(reg-event-db
  ::update-mapping-port-type
  (fn [db [_ id value]]
    (assoc-in db [::spec/port-mappings id :port-type] value)))


(reg-event-db
  ::add-volume
  (fn [db [_ id volume]]
    ; overwrite the id
    (assoc-in db [::spec/volumes id] (assoc volume :id id))))


(reg-event-db
  ::remove-volume
  (fn [db [_ id]]
    (update-in db [::spec/volumes] dissoc id)))


(reg-event-db
  ::update-volume-type
  (fn [db [_ id value]]
    (assoc-in db [::spec/volumes id :type] value)))


(reg-event-db
  ::update-volume-source
  (fn [db [_ id value]]
    (assoc-in db [::spec/volumes id :source] value)))


(reg-event-db
  ::update-volume-destination
  (fn [db [_ id value]]
    (assoc-in db [::spec/volumes id :destination] value)))


(reg-event-db
  ::update-volume-driver
  (fn [db [_ id value]]
    (assoc-in db [::spec/volumes id :driver] value)))


(reg-event-db
  ::update-volume-options
  (fn [db [_ id value]]
    (assoc-in db [::spec/volumes id :options] value)))
