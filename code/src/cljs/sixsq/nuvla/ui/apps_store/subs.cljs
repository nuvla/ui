(ns sixsq.nuvla.ui.apps-store.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.apps-store.spec :as spec]))

(reg-sub
  ::modules
  :-> ::spec/modules)
