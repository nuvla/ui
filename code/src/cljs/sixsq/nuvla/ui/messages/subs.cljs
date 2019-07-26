(ns sixsq.nuvla.ui.messages.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.messages.spec :as spec]))


(reg-sub
  ::messages
  (fn [db]
    (::spec/messages db)))


(reg-sub
  ::alert-message
  (fn [db]
    (::spec/alert-message db)))


(reg-sub
  ::alert-display
  (fn [db]
    (::spec/alert-display db)))


(reg-sub
  ::popup-open?
  (fn [db]
    (::spec/popup-open? db)))
