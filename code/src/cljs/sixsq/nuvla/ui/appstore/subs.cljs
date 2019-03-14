(ns sixsq.nuvla.ui.appstore.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.appstore.spec :as spec]))


(reg-sub
  ::modules
  ::spec/modules)


(reg-sub
  ::elements-per-page
  ::spec/elements-per-page)


(reg-sub
  ::page
  ::spec/page)
