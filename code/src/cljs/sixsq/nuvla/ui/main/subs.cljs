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
  ::is-small-device?
  :<- [::is-device? :mobile]
  :<- [::is-device? :tablet]
  (fn [[is-mobile? is-tablet?]]
    (boolean (or is-mobile? is-tablet?))))


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
  ::nav-path-first
  :<- [::nav-path]
  (fn [nav-path]
    (first nav-path)))


(reg-sub
  ::nav-url-active?
  :<- [::nav-path-first]
  (fn [nav-path-first [_ url]]
    (boolean (= nav-path-first url))))


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
  ::message
  (fn [db]
    (::spec/message db)))


(reg-sub
  ::next-refresh
  (fn [{:keys [::spec/actions-interval]} [_ action-id]]
    (get-in actions-interval [action-id :next-refresh])))


(reg-sub
  ::content-key
  (fn [db]
    (::spec/content-key db)))


(reg-sub
  ::pages
  (fn [db]
    (::spec/pages db)))


(reg-sub
  ::pages-list
  :<- [::pages]
  (fn [pages]
    (->> pages vals (filter :order) (sort-by :order))))


(reg-sub
  ::page-info
  :<- [::pages]
  (fn [pages [_ url]]
    (get pages url)))


(reg-sub
  ::config-map
  (fn [db]
    (::spec/config db)))


(reg-sub
  ::config
  :<- [::config-map]
  (fn [config-map [_ key]]
    (get config-map key)))


(reg-sub
  ::open-modal
  (fn [db]
    (::spec/open-modal db)))


(reg-sub
  ::modal-open?
  :<- [::open-modal]
  (fn [open-modal [_ modal-key]]
    (= open-modal modal-key)))


(reg-sub
  ::stripe
  (fn [db]
    (::spec/stripe db)))
