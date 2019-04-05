(ns sixsq.nuvla.ui.apps-component.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-component.spec :as spec]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::docker-image
  (fn [db [_ docker-image]]
    (assoc-in db [::apps-spec/module :content :image] docker-image)))


(reg-event-db
  ::architecture
  (fn [db [_ architecture]]
    (assoc-in db [::spec/architecture] architecture)))


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


(reg-event-db
  ::update-volume-read-only?
  (fn [db [_ id value]]
    (log/infof "checked: %s" value)
    (assoc-in db [::spec/volumes id :read-only?] value)))


(reg-event-db
  ::add-url
  (fn [db [_ id url]]
    ; overwrite the id
    (assoc-in db [::spec/urls id] (assoc url :id id))))


(reg-event-db
  ::remove-url
  (fn [db [_ id]]
    (update-in db [::spec/urls] dissoc id)))


(reg-event-db
  ::update-url-name
  (fn [db [_ id name]]
    (assoc-in db [::spec/urls id :name] name)))


(reg-event-db
  ::update-url-url
  (fn [db [_ id url]]
    (assoc-in db [::spec/urls id :url] url)))


(reg-event-db
  ::add-output-parameter
  (fn [db [_ id param]]
    ; overwrite the id
    (assoc-in db [::spec/output-parameters id] (assoc param :id id))))


(reg-event-db
  ::remove-output-parameter
  (fn [db [_ id]]
    (update-in db [::spec/output-parameters] dissoc id)))


(reg-event-db
  ::remove-output-parameter
  (fn [db [_ id]]
    (update-in db [::spec/output-parameters] dissoc id)))


(reg-event-db
  ::update-output-parameter-name
  (fn [db [_ id name]]
    (assoc-in db [::spec/output-parameters id :name] name)))


(reg-event-db
  ::update-output-parameter-description
  (fn [db [_ id description]]
    (assoc-in db [::spec/output-parameters id :description] description)))


(reg-event-db
  ::add-data-type
  (fn [db [_ id data-type]]
    ; overwrite the id
    (assoc-in db [::spec/data-types id] (assoc data-type :id id))))


(reg-event-db
  ::remove-data-type
  (fn [db [_ id]]
    (update-in db [::spec/data-types] dissoc id)))


(reg-event-db
  ::update-data-type
  (fn [db [_ id dt]]
    (assoc-in db [::spec/data-types id] dt)))
