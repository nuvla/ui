(ns sixsq.nuvla.ui.cimi-detail.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.cimi-detail.spec :as spec]))

(reg-sub
  ::loading?
  :-> ::spec/loading?)

(reg-sub
  ::resource
  :-> ::spec/resource)
