(ns sixsq.nuvla.ui.main.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.cimi.subs :as api-subs]
            [sixsq.nuvla.ui.main.spec :as spec]
            [sixsq.nuvla.ui.session.subs :as session-subs]))

(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))

(reg-sub
  ::not-found?
  (fn [db]
    (::spec/not-found? db)))

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
    (or is-mobile? is-tablet?)))

(reg-sub
  ::is-mobile-device?
  :<- [::is-device? :mobile]
  (fn [is-mobile?]
    is-mobile?))

(reg-sub
  ::sidebar-open?
  (fn [db]
    (::spec/sidebar-open? db)))

(reg-sub
  ::visible?
  (fn [db]
    (::spec/visible? db)))

(reg-sub
  ::changes-protection?
  (fn [db]
    (::spec/changes-protection? db)))

(reg-sub
  ::ignore-changes-modal
  (fn [db]
    (::spec/ignore-changes-modal db)))

(reg-sub
  ::revert-changes-modal
  (fn [db]
    (::spec/revert-changes-modal db)))

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

(reg-sub
  ::ui-version
  :-> ::spec/ui-version)

(reg-sub
  ::ui-version-modal-open?
  :<- [::ui-version]
  :-> :open-modal?)

(reg-sub
  ::ui-version-new-version
  :<- [::ui-version]
  :-> :new-version)

(reg-sub
  ::ui-version-current
  :<- [::ui-version]
  :-> :current-version)

(reg-sub
  ::app-loading?
  :<- [::api-subs/cloud-entry-point]
  :<- [::session-subs/session-loading?]
  (fn [[cep session-loading?]]
    (or (nil? cep) session-loading?)))
