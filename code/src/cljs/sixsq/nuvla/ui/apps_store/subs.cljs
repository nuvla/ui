(ns sixsq.nuvla.ui.apps-store.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.apps-store.spec :as spec]))


(reg-sub
  ::modules
  ::spec/modules)


(reg-sub
  ::published-modules
  ::spec/published-modules)


(reg-sub
  ::my-modules
  ::spec/my-modules)


; just the first few (no filter)
(reg-sub
  ::all-my-modules
  ::spec/all-my-modules)


(reg-sub
  ::elements-per-page
  ::spec/elements-per-page)


(reg-sub
  ::page
  ::spec/page)


(reg-sub
  ::full-text-search-published
  (fn [db]
    (::spec/full-text-search-published db)))


(reg-sub
  ::full-text-search-all-apps
  (fn [db]
    (::spec/full-text-search-all-apps db)))


(reg-sub
  ::full-text-search-my
  (fn [db]
    (::spec/full-text-search-my db)))


(reg-sub
  ::active-tab
  (fn [db]
    (::spec/active-tab db)))


(reg-sub
  ::state-selector
  (fn [db]
    (::spec/state-selector db)))
