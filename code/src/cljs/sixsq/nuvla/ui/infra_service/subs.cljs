(ns sixsq.nuvla.ui.infra-service.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.infra-service.spec :as spec]))


(reg-sub
  ::services
  ::spec/services)


(reg-sub
  ::elements-per-page
  ::spec/elements-per-page)


(reg-sub
  ::page
  ::spec/page)
