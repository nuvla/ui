(ns sixsq.slipstream.webui.application.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.application.spec :as application-spec]))


(reg-sub
  ::completed?
  ::application-spec/completed?)


(reg-sub
  ::module
  ::application-spec/module)


(reg-sub
  ::add-modal-visible?
  ::application-spec/add-modal-visible?)


(reg-sub
  ::add-data
  ::application-spec/add-data)


(reg-sub
  ::active-tab
  ::application-spec/active-tab)


