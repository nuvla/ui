(ns sixsq.nuvla.ui.pages.data.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.pages.data.spec :as spec]))

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
  ::modal-open?
  (fn [db]
    (::spec/modal-open? db)))

(reg-sub
  ::add-data-set-form
  (fn [db]
    (::spec/add-data-set-form db)))
