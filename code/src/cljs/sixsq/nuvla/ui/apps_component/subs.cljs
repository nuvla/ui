(ns sixsq.nuvla.ui.apps-component.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.apps-component.spec :as spec]))


(reg-sub
  ::port-mappings
  (fn [db]
    (::spec/port-mappings db)))

(reg-sub
  ::volumes
  (fn [db]
    (::spec/volumes db)))
