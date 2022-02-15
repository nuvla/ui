(ns sixsq.nuvla.ui.log-resource.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.log-resource.spec :as spec]))


(reg-sub
  ::resource-log
  (fn [db]
    (::spec/resource-log db)))


(reg-sub
  ::id
  (fn [db]
    (::spec/id db)))


(reg-sub
  ::since
  (fn [db]
    (::spec/since db)))


(reg-sub
  ::play?
  (fn [db]
    (::spec/play? db)))


(reg-sub
  ::components
  (fn [db]
    (::spec/components db)))


(reg-sub
  ::available-components
  (fn [db]
    (::spec/available-components db)))
