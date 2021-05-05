(ns sixsq.nuvla.ui.apps-component.subs
  (:require
    [re-frame.core :refer [reg-sub]]
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
  ::architectures
  (fn [db]
    (get-in db [::spec/module-component ::spec/architectures])))


(reg-sub
  ::docker-image
  (fn [db]
    (get-in db [::spec/module-component ::spec/image])))


(reg-sub
  ::module-component
  (fn [db]
    (get-in db [::spec/module-component])))
