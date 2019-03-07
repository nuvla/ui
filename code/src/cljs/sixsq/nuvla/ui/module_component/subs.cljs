(ns sixsq.nuvla.ui.module-component.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.module-component.spec :as spec]))


(reg-sub
  ::logo-url
  (fn [db]
    (::spec/logo-url db)))

(reg-sub
  ::name
  (fn [db]
    (::spec/name db)))

(reg-sub
  ::description
  (fn [db]
    (::spec/description db)))

(reg-sub
  ::default-logo-url
  (fn [db]
    (::spec/default-logo-url db)))

(reg-sub
  ::logo-url-modal-visible?
  (fn [db]
    (::spec/logo-url-modal-visible? db)))

(reg-sub
  ::port-mappings
  (fn [db]
    (::spec/port-mappings db)))

(reg-sub
  ::port-mappings-source
  (fn [db]
    (::spec/port-mappings db)))

(reg-sub
  ::page-changed?
  (fn [db]
    (::spec/page-changed? db)))
