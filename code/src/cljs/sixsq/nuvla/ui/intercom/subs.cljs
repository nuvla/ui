(ns sixsq.nuvla.ui.intercom.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.intercom.spec :as spec]))


(reg-sub
  ::events
  (fn [db]
    (::spec/events db)))
