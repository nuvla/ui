(ns sixsq.nuvla.ui.main.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.main.spec :as spec]))


(reg-sub
  ::iframe?
  (fn [db]
    (::spec/iframe? db)))


(reg-sub
  ::device
  (fn [db]
    (::spec/device db)))


(reg-sub
  ::is-device?
  :<- [::device]
  (fn [device [_ device-kw]]
    (= device device-kw)))


(reg-sub
  ::sidebar-open?
  (fn [db]
    (::spec/sidebar-open? db)))


(reg-sub
  ::visible?
  (fn [db]
    (::spec/visible? db)))


(reg-sub
  ::nav-path
  (fn [db]
    (::spec/nav-path db)))


(reg-sub
  ::nav-query-params
  (fn [db]
    (::spec/nav-query-params db)))


(reg-sub
  ::changes-protection?
  (fn [db]
    (::spec/changes-protection? db)))


(reg-sub
  ::ignore-changes-modal
  (fn [db]
    (::spec/ignore-changes-modal db)))

(reg-sub
  ::bootstrap-message
  (fn [db]
    (::spec/bootstrap-message db)))

(reg-sub
  ::welcome-message
  (fn [db]
    (::spec/welcome-message db)))
