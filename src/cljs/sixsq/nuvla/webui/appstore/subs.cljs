(ns sixsq.nuvla.webui.appstore.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.webui.appstore.spec :as spec]))


(reg-sub
  ::deployment-templates
  ::spec/deployment-templates)


(reg-sub
  ::elements-per-page
  ::spec/elements-per-page)


(reg-sub
  ::page
  ::spec/page)
