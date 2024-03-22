(ns sixsq.nuvla.ui.common-components.intercom.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.common-components.intercom.spec :as spec]))


(reg-sub
  ::events
  :-> ::spec/events)
