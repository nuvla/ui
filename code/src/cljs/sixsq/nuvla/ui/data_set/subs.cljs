(ns sixsq.nuvla.ui.data-set.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.data-set.spec :as spec]))


(reg-sub
  ::time-period
  (fn [db]
    (::spec/time-period db)))


(reg-sub
  ::data-set
  (fn [db]
    (::spec/data-set db)))


(reg-sub
  ::data-records
  (fn [db]
    (::spec/data-records db)))


(reg-sub
  ::data-objects
  (fn [db]
    (::spec/data-objects db)))


(reg-sub
  ::elements-per-page
  (fn [db]
    (::spec/elements-per-page db)))


(reg-sub
  ::page
  (fn [db]
    (::spec/page db)))


(reg-sub
  ::full-text-search
  (fn [db]
    (::spec/full-text-search db)))
