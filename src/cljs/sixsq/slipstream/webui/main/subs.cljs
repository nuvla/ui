(ns sixsq.slipstream.webui.main.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.main.spec :as spec]))


(reg-sub
  ::iframe?
  ::spec/iframe?)


(reg-sub
  ::device
  ::spec/device)


(reg-sub
  ::sidebar-open?
  ::spec/sidebar-open?)


(reg-sub
  ::visible?
  ::spec/visible?)


(reg-sub
  ::nav-path
  ::spec/nav-path)


(reg-sub
  ::nav-query-params
  ::spec/nav-query-params)
