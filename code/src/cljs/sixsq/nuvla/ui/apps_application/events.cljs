(ns sixsq.nuvla.ui.apps-application.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-application.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [taoensso.timbre :as log]))


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
  (fn [db [_ id docker-compose]]
    (assoc-in db [::spec/module-application ::spec/docker-compose] docker-compose)))


; Validation errors

(reg-event-db
  ::set-license-error
  (fn [db [_ key error?]]
    (let [errors  (::spec/license-errors db)
          is-set? (contains? errors key)
          reset?  (and (not error?) is-set?)
          set?    (and error? (not is-set?))]
      (cond-> db
              reset? (update ::spec/license-errors #(disj % key))
              set? (update ::spec/license-errors #(conj % key))))))
