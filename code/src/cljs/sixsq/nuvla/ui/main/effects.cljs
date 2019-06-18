(ns sixsq.nuvla.ui.main.effects
  (:require
    [re-frame.core :refer [dispatch reg-fx]]
    [taoensso.timbre :as log]))


(reg-fx
  ::bulk-actions-interval
  (fn [[dispatched-event-key actions-interval]]
    (doseq [id (keys actions-interval)]
      (dispatch [dispatched-event-key id]))))


(reg-fx
  ::open-new-window
  (fn [[url]]
    (.open js/window url "_blank" "noreferrer")))
