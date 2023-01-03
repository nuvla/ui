(ns sixsq.nuvla.ui.apps-applications-sets.events
  (:require
    [re-frame.core :refer [reg-event-db]]
    [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.plugins.module-selector :as module-selector]))

(reg-event-db
  ::clear-module
  (fn [db [_]]
    (merge db spec/defaults)))

(reg-event-db
  ::update-apps-set-name
  (fn [db [_ id name]]
    (assoc-in db [::spec/apps-sets id
                  ::spec/apps-set-name] name)))

(reg-event-db
  ::update-apps-set-description
  (fn [db [_ id description]]
    (assoc-in db [::spec/apps-sets id
                  ::spec/apps-set-description] description)))


(reg-event-db
  ::add-apps-set
  (fn [db]
    (let [id (-> db
                 ::spec/apps-sets
                 utils/sorted-map-new-idx)]
      (assoc-in db [::spec/apps-sets id] {:id                  id
                                          ::spec/apps-set-name ""
                                          ::spec/apps-selector (module-selector/build-spec)}))))

(reg-event-db
  ::remove-apps-set
  (fn [db [_ id]]
    (update db ::spec/apps-sets dissoc id)))


(reg-event-db
  ::set-apps-validation-error
  (fn [db [_ key error?]]
    (utils/set-reset-error db key error? ::spec/apps-validation-errors)))

(reg-event-db
  ::set-apps-selected
  (fn [db [_ id db-path]]
    (->> (module-selector/db-selected db db-path)
         (map (juxt :id identity) )
         (into {})
         (assoc-in db [::spec/apps-sets id ::spec/apps-selected]))))

(reg-event-db
  ::remove-app
  (fn [db [_ id module-id]]
    (update-in db [::spec/apps-sets id ::spec/apps-selected] dissoc module-id)))
