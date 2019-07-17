(ns sixsq.nuvla.ui.apps-component.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.apps-component.spec :as spec]
    [sixsq.nuvla.ui.docs.subs :as docs-subs]
    [taoensso.timbre :as log]))


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


(reg-sub
  ::architectures-options
  :<- [::docs-subs/document {:resource-metadata "resource-metadata/module-component"}]
  (fn [module-component-metadata _]
    (->> module-component-metadata :attributes
         (filter #(= (:name %) "architectures")) first :child-types first :value-scope :values
         (map (fn [arch] {:key arch, :value arch, :text arch})))))
