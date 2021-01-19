(ns sixsq.nuvla.ui.apps-store.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.apps-store.spec :as spec]))


(reg-sub
  ::modules
  ::spec/modules)


(reg-sub
  ::elements-per-page
  ::spec/elements-per-page)


(reg-sub
  ::page
  ::spec/page)


(reg-sub
  ::full-text-search
  (fn [db]
    (::spec/full-text-search db)))


(reg-sub
  ::active-tab-index
  (fn [db]
    (get-in db [::spec/active-tab-index])))
