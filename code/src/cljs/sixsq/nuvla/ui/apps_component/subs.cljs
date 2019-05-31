(ns sixsq.nuvla.ui.apps-component.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.apps-component.spec :as spec]))


(reg-sub
  ::image
  (fn [db]
    (get-in db [::spec/module-component ::spec/image])))


(reg-sub
  ::ports
  (fn [db]
    (get-in db [::spec/module-component ::spec/ports])))


(reg-sub
  ::mounts
  (fn [db]
    (get-in db [::spec/module-component ::spec/mounts])))


(reg-sub
  ::env-variables
  (fn [db]
    (get-in db [::spec/module-component ::spec/env-variables])))


(reg-sub
  ::urls
  (fn [db]
    (get-in db [::spec/module-component ::spec/urls])))


(reg-sub
  ::output-parameters
  (fn [db]
    (get-in db [::spec/module-component ::spec/output-parameters])))


(reg-sub
  ::architecture
  (fn [db]
    (get-in db [::spec/module-component ::spec/architecture])))


(reg-sub
  ::data-types
  (fn [db]
    (get-in db [::spec/module-component ::spec/data-types])))


(reg-sub
  ::docker-image
  (fn [db]
    (get-in db [::spec/module-component ::spec/image])))


(reg-sub
  ::module-component
  (fn [db]
    (get-in db [::spec/module-component])))
