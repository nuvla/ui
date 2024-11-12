(ns sixsq.nuvla.ui.pages.deployment-sets.subs
  (:require [re-frame.core :refer [reg-event-fx reg-sub]]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.pages.deployment-sets.spec :as spec]))

(reg-sub
  ::loading?
  :-> ::spec/loading?)

(reg-sub
  ::deployment-sets
  :-> ::spec/deployment-sets)

(reg-sub
  ::deployment-sets-resources
  :<- [::deployment-sets]
  (fn [deployment-sets-response]
    (vec (:resources deployment-sets-response))))

(reg-sub
  ::deployment-sets-summary
  :-> ::spec/deployment-sets-summary)

(reg-sub
  ::state-selector
  :-> ::spec/state-selector)

(def events-table-col-configs-local-storage-key "nuvla.ui.table.events.column-configs")

(reg-sub
  ::table-current-cols
  :<- [::main-subs/current-cols events-table-col-configs-local-storage-key ::events-columns-ordering]
  identity)

(main-events/reg-set-current-cols-event-fx
  ::set-table-current-cols-main events-table-col-configs-local-storage-key)

(reg-event-fx
  ::set-table-current-cols
  (fn [_ [_ columns]]
    {:fx [[:dispatch [::set-table-current-cols-main ::events-columns-ordering columns]]]}))
