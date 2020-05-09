(ns sixsq.nuvla.ui.intercom.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.intercom.spec :as spec]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [taoensso.timbre :as log]))


(reg-sub
  ::app-id
  (fn [db]
    (::main-spec/intercom-api-id db)))


(reg-sub
  ::events
  (fn [db]
    (::spec/events db)))
