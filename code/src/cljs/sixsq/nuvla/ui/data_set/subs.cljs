(ns sixsq.nuvla.ui.data-set.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.data-set.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as utils-general]))


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
