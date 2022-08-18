(ns sixsq.nuvla.ui.plugins.helpers
  (:require
    [re-frame.core :refer [dispatch subscribe reg-sub reg-event-db]]))

(reg-sub
  ::retrieve
  (fn [db [_ location-kw k]]
    (get-in db [location-kw k])))

(reg-sub
  ::retrieve2
  (fn [db [_ db-path k]]
    (get-in db (conj db-path k))))

(reg-event-db
  ::set2
  (fn [db [_ db-path k v]]
    (assoc-in db (conj db-path k) v)))

(reg-event-db
  ::set
  (fn [db [_ location-kw k v]]
    (assoc-in db [location-kw k] v)))
