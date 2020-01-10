(ns sixsq.nuvla.ui.profile.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.profile.spec :as spec]))


(reg-sub
  ::credential-password
  (fn [db]
    (::spec/credential-password db)))


(reg-sub
  ::open-modal
  ::spec/open-modal)


(reg-sub
  ::error-message
  ::spec/error-message)
