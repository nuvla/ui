(ns sixsq.nuvla.ui.module-component.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.module-component.spec :as spec]))


(reg-sub
  ::port-mappings
  (fn [db]
    (::spec/port-mappings db)))

(reg-sub
  ::volumes
  (fn [db]
    (::spec/volumes db)))
