(ns sixsq.nuvla.ui.messages.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.messages.spec :as messages-spec]))


(reg-sub
  ::messages
  ::messages-spec/messages)


(reg-sub
  ::alert-message
  ::messages-spec/alert-message)


(reg-sub
  ::alert-display
  ::messages-spec/alert-display)


(reg-sub
  ::popup-open?
  ::messages-spec/popup-open?)
