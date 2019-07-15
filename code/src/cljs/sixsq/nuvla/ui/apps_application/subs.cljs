(ns sixsq.nuvla.ui.apps-application.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.apps-application.spec :as spec]
    [taoensso.timbre :as log]))


(reg-sub
  ::docker-compose
  (fn [db]
    (get-in db [::spec/module-application ::spec/docker-compose])))


(reg-sub
  ::module-application
  (fn [db]
    (get-in db [::spec/module-application])))
