(ns sixsq.nuvla.ui.data-set.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.data-set.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [clojure.string :as str]))


(reg-sub
  ::time-period
  ::spec/time-period)


(reg-sub
  ::data-set
  ::spec/data-set)


(reg-sub
  ::not-found?
  ::spec/not-found?)


(reg-sub
  ::data-records
  ::spec/data-records)


(reg-sub
  ::data-objects
  ::spec/data-objects)


(reg-sub
  ::elements-per-page
  ::spec/elements-per-page)


(reg-sub
  ::page
  ::spec/page)


(reg-sub
  ::full-text-search
  ::spec/full-text-search)


(reg-sub
  ::editable?
  (fn [db]
    (utils-general/can-edit? (::spec/data-set db))))


(reg-sub
  ::data-record-filter
  (fn [db]
    (::spec/data-record-filter db)))


(reg-sub
  ::map-selection
  (fn [db]
    (::spec/map-selection db)))


(reg-sub
  ::suggest-update-data-record-filter?
  (fn [{:keys [::spec/data-set
               ::spec/data-record-filter]}]
    (and (some? data-set)
         (utils-general/can-edit? data-set)
         (not= (:data-record-filter data-set) data-record-filter))))

(reg-sub
  ::suggest-new-data-set?
  (fn [{:keys [::spec/data-set
               ::spec/data-record-filter]}]
    (and (nil? data-set) (not (str/blank? data-record-filter)))))


(reg-sub
  ::selected-data-record-ids
  (fn [db]
    (::spec/selected-data-record-ids db)))


(reg-sub
  ::geo-opeation
  (fn [db]
    (::spec/geo-operation db)))

(reg-sub
  ::geo-opeation-active?
  :<- [::geo-opeation]
  (fn [geo-operation [_ op]]
    (= geo-operation op)))
