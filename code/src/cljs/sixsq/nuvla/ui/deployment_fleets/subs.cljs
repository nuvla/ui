(ns sixsq.nuvla.ui.deployment-fleets.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.deployment-fleets.spec :as spec]))

(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))

(reg-sub
  ::full-text-search
  (fn [db]
    (::spec/full-text-search db)))

(reg-sub
  ::deployment-fleets
  (fn [db]
    (::spec/deployment-fleets db)))

(reg-sub
  ::deployment-fleets-summary
  (fn [db]
    (::spec/deployment-fleets-summary db)))

(reg-sub
  ::state-selector
  (fn [db]
    (::spec/state-selector db)))
