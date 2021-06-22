(ns sixsq.nuvla.ui.acl.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.acl.spec :as spec]))


(reg-sub
  ::groups
  (fn [{:keys [::spec/groups]}]
    groups))
