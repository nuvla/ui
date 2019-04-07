(ns sixsq.nuvla.ui.apps-component.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.apps-component.spec :as spec]))


(reg-sub
  ::ports
  (fn [db]
    (::spec/ports db)))


(reg-sub
  ::mounts
  (fn [db]
    (::spec/mounts db)))


(reg-sub
  ::urls
  (fn [db]
    (::spec/urls db)))


(reg-sub
  ::output-parameters
  (fn [db]
    (::spec/output-parameters db)))


(reg-sub
  ::architecture
  (fn [db]
    (::spec/architecture db)))


(reg-sub
  ::data-types
  (fn [db]
    (::spec/data-types db)))


(reg-sub
  ::docker-image
  (fn [db]
    (::spec/image db)))
