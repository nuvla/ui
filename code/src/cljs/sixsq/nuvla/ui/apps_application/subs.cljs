(ns sixsq.nuvla.ui.apps-application.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.apps-application.spec :as spec]))


(reg-sub
  ::docker-compose
  (fn [db]
    (get-in db [::spec/module-application ::spec/docker-compose])))


(reg-sub
  ::files
  (fn [db]
    (get-in db [::spec/module-application ::spec/files])))


(reg-sub
  ::module-application
  (fn [db]
    (get-in db [::spec/module-application])))


(reg-sub
  ::active-tab-index
  (fn [db]
    (get-in db [::spec/active-tab-index])))


(reg-sub
  ::license-error?
  (fn [db]
    (not (empty? (get-in db [::spec/license-errors])))))
