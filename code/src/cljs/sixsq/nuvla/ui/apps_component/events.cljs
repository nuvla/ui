(ns sixsq.nuvla.ui.apps-component.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-component.spec :as spec]))


(reg-event-db
  ::clear-module
  (fn [db [_]]
    (merge db spec/defaults)))


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


; Docker image

(reg-event-db
  ::update-docker-image
  (fn [db [_ id repo_image]]
    (let [temp       (if (string? repo_image) (str/split repo_image "/") nil)
          two_parts? (= (count temp) 2)
          repository (if two_parts? (first temp) nil)
          image-name (if two_parts? (second temp) repo_image)]
      (-> db
          (assoc-in [::spec/module-component ::spec/image ::spec/image-name] image-name)
          (assoc-in [::spec/module-component ::spec/image ::spec/repository] repository)))))


(reg-event-db
  ::update-docker-registry
  (fn [db [_ id registry]]
    (assoc-in db [::spec/module-component ::spec/image ::spec/registry] registry)))


(reg-event-db
  ::update-docker-tag
  (fn [db [_ id tag]]
    (assoc-in db [::spec/module-component ::spec/image ::spec/tag] tag)))
