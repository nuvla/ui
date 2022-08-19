(ns sixsq.nuvla.ui.plugins.helpers
  (:require
    [re-frame.core :refer [reg-sub reg-event-db]]))

(reg-sub
  ::retrieve
  (fn [db [_ db-path k]]
    (get-in db (conj db-path k))))

(reg-event-db
  ::set
  (fn [db [_ db-path & {:as m}]]
    (update-in db db-path merge m)))
