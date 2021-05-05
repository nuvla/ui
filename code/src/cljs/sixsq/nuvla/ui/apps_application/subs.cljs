(ns sixsq.nuvla.ui.apps-application.subs
  (:require
    [re-frame.core :refer [reg-sub]]
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
    #_ :clj-kondo/ignore
    (not (empty? (::spec/license-validation-errors db)))))


(reg-sub
  ::docker-compose-validation-error?
  (fn [db]
    #_ :clj-kondo/ignore
    (not (empty? (::spec/docker-compose-validation-errors db)))))


(reg-sub
  ::module-requires-user-rights
  (fn [db]
    (get-in db [::spec/module-application ::spec/requires-user-rights])))
