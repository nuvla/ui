(ns sixsq.nuvla.ui.data.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.data.spec :as spec]))


(reg-sub
  ::credentials
  (fn [db]
    (::spec/credentials db)))


(reg-sub
  ::application-select-visible?
  (fn [db]
    (::spec/application-select-visible? db)))


(reg-sub
  ::loading-applications?
  (fn [db]
    (::spec/loading-applications? db)))


(reg-sub
  ::applications
  (fn [db]
    (::spec/applications db)))


(reg-sub
  ::selected-application-id
  (fn [db]
    (::spec/selected-application-id db)))


(reg-sub
  ::elements-per-page
  (fn [db]
    (::spec/elements-per-page db)))


(reg-sub
  ::page
  (fn [db]
    (::spec/page db)))


(reg-sub
  ::total
  (fn [db]
    (::spec/total db)))


(reg-sub
  ::counts
  (fn [db]
    (::spec/counts db)))


(reg-sub
  ::sizes
  (fn [db]
    (::spec/sizes db)))


(reg-sub
  ::data-sets
  (fn [db]
    (::spec/data-sets db)))


(reg-sub
  ::selected-data-set-ids
  (fn [db]
    (::spec/selected-data-set-ids db)))


(reg-sub
  ::full-text-search
  (fn [db]
    (::spec/full-text-search db)))
