(ns sixsq.nuvla.ui.apps-component.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-component.spec :as spec]))


(reg-event-db
  ::clear-module
  (fn [db [_]]
    (-> db
        (assoc-in [::spec/module-component ::spec/ports] {})
        (assoc-in [::spec/module-component ::spec/mounts] {})
        (assoc-in [::spec/module-component ::spec/urls] {})
        (assoc-in [::spec/module-component ::spec/output-parameters] {})
        (assoc-in [::spec/module-component ::spec/data-types] {})
        (assoc-in [::spec/module-component ::spec/architectures] ["amd64"])
        (assoc-in [::spec/module-component ::spec/env-variables] {})
        (assoc-in [::spec/module-component ::spec/image] {}))))

(reg-event-db
  ::architectures
  (fn [db [_ architectures]]
    (assoc-in db [::spec/module-component ::spec/architectures] architectures)))


; Ports

(reg-event-db
  ::add-port
  (fn [db [_ id mapping]]
    ; overwrite the id
    (assoc-in db [::spec/module-component ::spec/ports id] (assoc mapping :id id))))


(reg-event-db
  ::remove-port
  (fn [db [_ id]]
    (update-in db [::spec/module-component ::spec/ports] dissoc id)))


(defn numeric? [s]
  (let [is-num? (int? s)
        num     (js/parseInt s)
        num-str (str num)]
    (or is-num? (= num-str s))))


(reg-event-db
  ::update-port-published
  (fn [db [_ id value]]
    (let [value-int (if
                      (numeric? value)
                      (js/parseInt value)
                      (if (empty? value) nil value))]
      (assoc-in db [::spec/module-component ::spec/ports id ::spec/published-port] value-int))))


(reg-event-db
  ::update-port-target
  (fn [db [_ id value]]
    (let [value-int (if (numeric? value) (js/parseInt value) value)]
      (assoc-in db [::spec/module-component ::spec/ports id ::spec/target-port] value-int))))


(reg-event-db
  ::update-port-protocol
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-component ::spec/ports id ::spec/protocol] value)))


; Volumes (mounts)

(reg-event-db
  ::add-mount
  (fn [db [_ id mount]]
    ; overwrite the id
    (assoc-in db [::spec/module-component ::spec/mounts id] (assoc mount :id id))))


(reg-event-db
  ::remove-mount
  (fn [db [_ id]]
    (update-in db [::spec/module-component ::spec/mounts] dissoc id)))


(reg-event-db
  ::update-mount-type
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-component ::spec/mounts id ::spec/mount-type] value)))


(reg-event-db
  ::update-mount-source
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-component ::spec/mounts id ::spec/mount-source] value)))


(reg-event-db
  ::update-mount-target
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-component ::spec/mounts id ::spec/mount-target] value)))


;(reg-event-db
;  ::update-mount-options
;  (fn [db [_ id value]]
;    (assoc-in db [::spec/module-component ::spec/mounts id ::spec/mount-options] value)))


(reg-event-db
  ::update-mount-read-only?
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-component ::spec/mounts id ::spec/mount-read-only] value)))


; Environmental variables

(reg-event-db
  ::add-env-variable
  (fn [db [_ id env-variable]]
    ; overwrite the id
    (assoc-in db [::spec/module-component ::spec/env-variables id] (assoc env-variable :id id))))


(reg-event-db
  ::remove-env-variable
  (fn [db [_ id]]
    (update-in db [::spec/module-component ::spec/env-variables] dissoc id)))


(reg-event-db
  ::update-env-name
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-component ::spec/env-variables id ::spec/env-name] (str/upper-case value))))


(reg-event-db
  ::update-env-value
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-component ::spec/env-variables id ::spec/env-value]
              (when-not (str/blank? value) value))))


(reg-event-db
  ::update-env-description
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-component ::spec/env-variables id ::spec/env-description] value)))


(reg-event-db
  ::update-env-required
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-component ::spec/env-variables id ::spec/env-required] value)))


(reg-event-db
  ::add-url
  (fn [db [_ id url]]
    ; overwrite the id
    (assoc-in db [::spec/module-component ::spec/urls id] (assoc url :id id))))


(reg-event-db
  ::remove-url
  (fn [db [_ id]]
    (update-in db [::spec/module-component ::spec/urls] dissoc id)))


(reg-event-db
  ::update-url-name
  (fn [db [_ id name]]
    (assoc-in db [::spec/module-component ::spec/urls id ::spec/url-name] name)))


(reg-event-db
  ::update-url-url
  (fn [db [_ id url]]
    (assoc-in db [::spec/module-component ::spec/urls id ::spec/url] url)))


(reg-event-db
  ::add-output-parameter
  (fn [db [_ id param]]
    ; overwrite the id
    (assoc-in db [::spec/module-component ::spec/output-parameters id] (assoc param :id id))))


(reg-event-db
  ::remove-output-parameter
  (fn [db [_ id]]
    (update-in db [::spec/module-component ::spec/output-parameters] dissoc id)))


(reg-event-db
  ::update-output-parameter-name
  (fn [db [_ id name]]
    (assoc-in db [::spec/module-component ::spec/output-parameters id
                  ::spec/output-parameter-name] name)))


(reg-event-db
  ::update-output-parameter-description
  (fn [db [_ id description]]
    (assoc-in db [::spec/module-component ::spec/output-parameters id
                  ::spec/output-parameter-description] description)))


(reg-event-db
  ::add-data-type
  (fn [db [_ id data-type]]
    ; overwrite the id
    (assoc-in db [::spec/module-component ::spec/data-types id] (assoc data-type :id id))))


(reg-event-db
  ::remove-data-type
  (fn [db [_ id]]
    (update-in db [::spec/module-component ::spec/data-types] dissoc id)))


(reg-event-db
  ::update-data-type
  (fn [db [_ id dt]]
    (assoc-in db [::spec/module-component ::spec/data-types id] {:id id ::spec/data-type dt})))


; Docker image

(reg-event-db
  ::update-docker-image-name
  (fn [db [_ id image-name]]
    (assoc-in db [::spec/module-component ::spec/image ::spec/image-name] image-name)))


(reg-event-db
  ::update-docker-repository
  (fn [db [_ id repository]]
    (assoc-in db [::spec/module-component ::spec/image ::spec/repository] repository)))


(reg-event-db
  ::update-docker-registry
  (fn [db [_ id registry]]
    (assoc-in db [::spec/module-component ::spec/image ::spec/registry] registry)))


(reg-event-db
  ::update-docker-tag
  (fn [db [_ id tag]]
    (assoc-in db [::spec/module-component ::spec/image ::spec/tag] tag)))
