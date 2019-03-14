(ns sixsq.nuvla.ui.module-project.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.module-project.spec :as spec]))


(reg-sub
  ::logo-url
  (fn [db]
    (::spec/logo-url db)))

(reg-sub
  ::name
  (fn [db]
    (::spec/name db)))

(reg-sub
  ::parent
  (fn [db]
    (::spec/parent db)))

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
  ::save-modal-visible?
  (fn [db]
    (::spec/save-modal-visible? db)))

(reg-sub
  ::port-mappings
  (fn [db]
    (::spec/port-mappings db)))

(reg-sub
  ::volumes
  (fn [db]
    (::spec/volumes db)))

(reg-sub
  ::page-changed?
  (fn [db]
    (::spec/page-changed? db)))
