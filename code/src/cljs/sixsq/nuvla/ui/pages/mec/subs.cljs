 (ns sixsq.nuvla.ui.pages.mec.subs
   (:require [re-frame.core :refer [reg-sub]]
             [sixsq.nuvla.ui.pages.mec.spec :as spec]))

 (reg-sub
   ::mepms
   :-> ::spec/mepms)

 (reg-sub
   ::subscriptions
   :-> ::spec/subscriptions)

 (reg-sub
   ::selected-mepm-id
   :-> ::spec/selected-mepm-id)

 (reg-sub
   ::selected-mepm
   :-> ::spec/selected-mepm)

 (reg-sub
   ::loading-mepms?
   :-> ::spec/loading-mepms?)

 (reg-sub
   ::loading-subscriptions?
   :-> ::spec/loading-subscriptions?)

 (reg-sub
   ::loading-selected-mepm?
   :-> ::spec/loading-selected-mepm?)

 (reg-sub
   ::loading?
   :<- [::loading-mepms?]
   :<- [::loading-subscriptions?]
   :<- [::loading-selected-mepm?]
   (fn [[loading-mepms? loading-subscriptions? loading-selected-mepm?]]
     (or loading-mepms? loading-subscriptions? loading-selected-mepm?)))
