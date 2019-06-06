(ns sixsq.nuvla.ui.messages.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.messages.spec :as messages-spec]))


(reg-sub
  ::messages
  (fn [db]
    (::messages-spec/messages db)))


(reg-sub
  ::alert-message
  (fn [db]
    (::messages-spec/alert-message db)))


(reg-sub
  ::alert-display
  (fn [db]
    (::messages-spec/alert-display db)))


(reg-sub
  ::popup-open?
  (fn [db]
    (::messages-spec/popup-open? db)))
