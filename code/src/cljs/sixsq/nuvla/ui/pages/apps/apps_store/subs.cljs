(ns sixsq.nuvla.ui.pages.apps.apps-store.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.pages.apps.apps-store.spec :as spec]))

(reg-sub
  ::modules
  :-> ::spec/modules)
