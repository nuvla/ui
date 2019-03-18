(ns sixsq.nuvla.ui.application.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.application.spec :as spec]))


(reg-sub
  ::completed?
  ::spec/completed?)


(reg-sub
  ::module
  ::spec/module)


(reg-sub
  ::add-modal-visible?
  ::spec/add-modal-visible?)


(reg-sub
  ::add-data
  ::spec/add-data)


(reg-sub
  ::page-changed?
  ::spec/page-changed?)


(reg-sub
  ::save-modal-visible?
  ::spec/save-modal-visible?)


(reg-sub
  ::default-logo-url
  ::spec/default-logo-url)


(reg-sub
  ::logo-url-modal-visible?
  ::spec/logo-url-modal-visible?)
