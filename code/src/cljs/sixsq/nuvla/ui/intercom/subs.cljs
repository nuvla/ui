(ns sixsq.nuvla.ui.intercom.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.intercom.spec :as spec]
    [taoensso.timbre :as log]))


(reg-sub
  ::app-id
  (fn [db]
    (::spec/app-id db)))


(reg-sub
  ::events
  (fn [db]
    (log/info "from sub: " (::spec/events db))
    (::spec/events db)))
