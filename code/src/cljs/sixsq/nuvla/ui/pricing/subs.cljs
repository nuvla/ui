(ns sixsq.nuvla.ui.pricing.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.pricing.spec :as spec]))


(reg-sub
  ::stripe
  (fn [db]
    (::spec/stripe db)))


(reg-sub
  ::subscription
  (fn [db]
    (::spec/subscription db)))


(reg-sub
  ::error
  (fn [db]
    (::spec/error db)))


(reg-sub
  ::processing?
  (fn [db]
    (::spec/processing? db)))
