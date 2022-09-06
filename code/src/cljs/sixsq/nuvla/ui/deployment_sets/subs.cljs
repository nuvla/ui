(ns sixsq.nuvla.ui.deployment-sets.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.deployment-sets.spec :as spec]))

(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))

(reg-sub
  ::deployment-sets
  (fn [db]
    (::spec/deployment-sets db)))

(reg-sub
  ::deployment-sets-summary
  (fn [db]
    (::spec/deployment-sets-summary db)))

(reg-sub
  ::state-selector
  (fn [db]
    (::spec/state-selector db)))
