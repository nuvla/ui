(ns sixsq.nuvla.ui.apps.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.apps.spec :as spec]))


(reg-sub
  ::completed?
  ::spec/completed?)


(reg-sub
  ::is-new?
  ::spec/is-new?)


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
