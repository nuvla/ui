(ns sixsq.nuvla.ui.ocre.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.ocre.spec :as spec]))


(reg-sub
  ::distributors-terms
  (fn [db]
    (::spec/distributor-terms db)))


(reg-sub
  ::global-aggregations
  (fn [db]
    (::spec/global-aggregations db)))


(reg-sub
  ::count-ids
  :<- [::global-aggregations]
  (fn [global-aggregations]
    (-> global-aggregations :value_count:id :value)))


(reg-sub
  ::cardinality-distributor
  :<- [::global-aggregations]
  (fn [global-aggregations]
    (-> global-aggregations :cardinality:distributor :value)))


(reg-sub
  ::cardinality-supplier
  :<- [::global-aggregations]
  (fn [global-aggregations]
    (-> global-aggregations :cardinality:supplier :value)))


(reg-sub
  ::cardinality-platform
  :<- [::global-aggregations]
  (fn [global-aggregations]
    (-> global-aggregations :cardinality:platform :value)))


(reg-sub
  ::platforms-radar
  (fn [db]
    (::spec/platforms-radar db)))
