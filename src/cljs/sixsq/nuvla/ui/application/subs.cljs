(ns sixsq.nuvla.ui.application.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.application.spec :as application-spec]))


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


