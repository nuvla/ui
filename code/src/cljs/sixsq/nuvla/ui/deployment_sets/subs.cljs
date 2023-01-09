(ns sixsq.nuvla.ui.deployment-sets.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.deployment-sets.spec :as spec]))

(reg-sub
  ::loading?
  :-> ::spec/loading?)

(reg-sub
  ::deployment-sets
  :-> ::spec/deployment-sets)

(reg-sub
  ::deployment-sets-summary
  :-> ::spec/deployment-sets-summary)

(reg-sub
  ::state-selector
  :-> ::spec/state-selector)
