(ns sixsq.nuvla.ui.apps-application.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.apps-application.spec :as spec]))


(reg-sub
  ::docker-compose
  :<- [::module-application]
  (fn [module]
    (::spec/docker-compose module)))


(reg-sub
  ::files
  (fn [db]
    (get-in db [::spec/module-application ::spec/files])))


(reg-sub
  ::module-application
  (fn [db]
    (::spec/module-application db)))


(reg-sub
  ::license-error?
  (fn [db]
    (not (empty? (::spec/license-validation-errors db)))))


(reg-sub
  ::docker-compose-validation-error?
  (fn [db]
    (not (empty? (::spec/docker-compose-validation-errors db)))))
