(ns sixsq.nuvla.ui.apps-applications-set.events
  (:require
    [re-frame.core :refer [reg-event-db]]
    [sixsq.nuvla.ui.apps-applications-set.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as utils]))


(reg-event-db
  ::clear-module
  (fn [db [_]]
    (merge db spec/defaults)))


; Files

(reg-event-db
  ::add-file
  (fn [db [_ _]]
    (let [id (-> db
                 (get-in [::spec/module-application ::spec/files])
                 utils/sorted-map-new-idx)]
      (assoc-in db [::spec/module-application ::spec/files id] {:id id, ::spec/file-content ""}))))


(reg-event-db
  ::remove-file
  (fn [db [_ id]]
    (update-in db [::spec/module-application ::spec/files] dissoc id)))


(reg-event-db
  ::update-file-name
  (fn [db [_ id file-name]]
    (assoc-in db [::spec/module-application ::spec/files id
                  ::spec/file-name] file-name)))


(reg-event-db
  ::update-file-content
  (fn [db [_ id file-content]]
    (assoc-in db [::spec/module-application ::spec/files id
                  ::spec/file-content] file-content)))


; Docker compose

(reg-event-db
  ::update-docker-compose
  (fn [db [_ docker-compose]]
    (assoc-in db [::spec/module-application ::spec/docker-compose] docker-compose)))


; Validation errors

(reg-event-db
  ::set-license-validation-error
  (fn [db [_ key error?]]
    (utils/set-reset-error db key error? ::spec/license-validation-errors)))


(reg-event-db
  ::set-docker-validation-error
  (fn [db [_ key error?]]
    (utils/set-reset-error db key error? ::spec/docker-compose-validation-errors)))


(reg-event-db
  ::set-configuration-validation-error
  (fn [db [_ key error?]]
    (utils/set-reset-error db key error? ::spec/configuration-validation-errors)))

; Requires user rights

(reg-event-db
  ::update-requires-user-rights
  (fn [db [_ value]]
    (assoc-in db [::spec/module-application ::spec/requires-user-rights] value)))

;(reg-event-db
;  ::add-file
;  (fn [db [_ _]]
;    (let [id (-> db
;                 (get-in [::spec/module-application ::spec/files])
;                 utils/sorted-map-new-idx)]
;      (assoc-in db [::spec/module-application ::spec/files id] {:id id, ::spec/file-content ""}))))
;
;
;(reg-event-db
;  ::remove-file
;  (fn [db [_ id]]
;    (update-in db [::spec/module-application ::spec/files] dissoc id)))
;

(reg-event-db
  ::update-apps-set-name
  (fn [db [_ id name]]
    (assoc-in db [::spec/apps-groups id
                  ::spec/apps-group-name] name)))


(reg-event-db
  ::add-apps-set
  (fn [db]
    (let [id (-> db
                 ::spec/apps-groups
                 utils/sorted-map-new-idx)]
      (js/console.warn id)
      (assoc-in db [::spec/apps-groups id] {:id id, ::spec/file-content ""}))))

(reg-event-db
  ::remove-apps-set
  (fn [db [_ id]]
    (update db ::spec/apps-groups dissoc id)))
