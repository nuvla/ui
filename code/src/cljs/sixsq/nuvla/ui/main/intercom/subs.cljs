(ns sixsq.nuvla.ui.main.intercom.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.main.intercom.spec :as spec]))


(reg-sub
  ::events
  (fn [db]
    (::spec/events db)))
