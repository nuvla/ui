(ns sixsq.nuvla.ui.plugins.helpers
  (:require
    [re-frame.core :refer [reg-sub reg-event-db]]
    [cljs.spec.alpha :as s]))

(s/def ::db-path (s/* keyword?))
(s/def ::change-event (s/cat :dispatch-key keyword? :data (s/* any?)))

(reg-sub
  ::retrieve
  (fn [db [_ db-path k]]
    (get-in db (conj db-path k))))

(reg-event-db
  ::set
  (fn [db [_ db-path & {:as m}]]
    (update-in db db-path merge m)))
