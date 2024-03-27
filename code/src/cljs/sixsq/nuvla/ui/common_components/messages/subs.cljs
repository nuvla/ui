(ns sixsq.nuvla.ui.common-components.messages.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.common-components.messages.spec :as spec]))


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

(reg-sub
  ::show-clear-all?
  :<- [::messages]
  (fn [messages]
    (boolean (some (fn [{message-type :type}] (not= message-type :notif)) messages))))
