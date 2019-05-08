(ns sixsq.nuvla.ui.main.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.main.spec :as spec]))


(reg-sub
  ::iframe?
  ::spec/iframe?)


(reg-sub
  ::device
  ::spec/device)


(reg-sub
  ::is-device?
  :<- [::device]
  (fn [device [_ device-kw]]
    (= device device-kw)))


(reg-sub
  ::sidebar-open?
  ::spec/sidebar-open?)


(reg-sub
  ::visible?
  ::spec/visible?)


(reg-sub
  ::nav-path
  ::spec/nav-path)


(reg-sub
  ::nav-query-params
  ::spec/nav-query-params)


(reg-sub
  ::changes-protection?
  ::spec/changes-protection?)


(reg-sub
  ::ignore-changes-modal
  (fn [db]
    (::spec/ignore-changes-modal db)))

(reg-sub
  ::bootstrap-message
  (fn [db]
    (::spec/bootstrap-message db)))
