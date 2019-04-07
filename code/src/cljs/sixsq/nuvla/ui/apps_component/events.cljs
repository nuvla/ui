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

; Ports

(reg-event-db
  ::add-port
  (fn [db [_ id mapping]]
    ; overwrite the id
    (assoc-in db [::spec/ports id] (assoc mapping :id id))))


(reg-event-db
  ::remove-port
  (fn [db [_ id]]
    (update-in db [::spec/ports] dissoc id)))


(reg-event-db
  ::update-port-published
  (fn [db [_ id value]]
    (assoc-in db [::spec/ports id :published-port] value)))


(reg-event-db
  ::update-port-target
  (fn [db [_ id value]]
    (assoc-in db [::spec/ports id :target-port] value)))


(reg-event-db
  ::update-port-protocol
  (fn [db [_ id value]]
    (assoc-in db [::spec/ports id :protocol] value)))


; Volumes (mounts)

(reg-event-db
  ::add-mount
  (fn [db [_ id mount]]
    ; overwrite the id
    (assoc-in db [::spec/mounts id] (assoc mount :id id))))


(reg-event-db
  ::remove-mount
  (fn [db [_ id]]
    (update-in db [::spec/mounts] dissoc id)))


(reg-event-db
  ::update-mount-type
  (fn [db [_ id value]]
    (assoc-in db [::spec/mounts id :mount-type] value)))


(reg-event-db
  ::update-mount-source
  (fn [db [_ id value]]
    (assoc-in db [::spec/mounts id :source] value)))


(reg-event-db
  ::update-mount-target
  (fn [db [_ id value]]
    (assoc-in db [::spec/mounts id :target] value)))


(reg-event-db
  ::update-mount-options
  (fn [db [_ id value]]
    (assoc-in db [::spec/mounts id :options] value)))


(reg-event-db
  ::update-mount-read-only?
  (fn [db [_ id value]]
    (log/infof "checked: %s" value)
    (assoc-in db [::spec/mounts id :read-only] value)))


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
    (assoc-in db [::spec/data-types id] {:id id :data-type dt})))


; Docker image

(reg-event-db
  ::update-docker-image-name
  (fn [db [_ image-name]]
    (assoc-in db [::apps-spec/module :content :image :image-name] image-name)))


(reg-event-db
  ::update-docker-repository
  (fn [db [_ repository]]
    (assoc-in db [::apps-spec/module :content :image :repository] repository)))


(reg-event-db
  ::update-docker-registry
  (fn [db [_ registry]]
    (assoc-in db [::apps-spec/module :content :image :registry] registry)))


(reg-event-db
  ::update-docker-tag
  (fn [db [_ tag]]
    (assoc-in db [::apps-spec/module :content :image :tag] tag)))
