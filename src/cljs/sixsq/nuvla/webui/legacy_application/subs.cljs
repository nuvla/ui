(ns sixsq.nuvla.webui.legacy-application.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.webui.legacy-application.spec :as application-spec]))


(reg-sub
  ::completed?
  ::application-spec/completed?)


(reg-sub
  ::module-id
  ::application-spec/module-id)


(reg-sub
  ::module
  ::application-spec/module)
