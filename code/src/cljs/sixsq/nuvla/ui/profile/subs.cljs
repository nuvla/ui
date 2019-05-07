(ns sixsq.nuvla.ui.profile.subs
  (:require
    [re-frame.core :refer [dispatch reg-sub subscribe]]
    [sixsq.nuvla.ui.profile.spec :as spec]))


(reg-sub
  ::credential-password
  (fn [db]
    (::spec/credential-password db)))


; TODO: make more specific
(reg-sub
  ::open-modal
  ::spec/open-modal)


(reg-sub
  ::error-message
  ::spec/error-message)


(reg-sub
  ::form-data
  (fn [db]
    (::spec/form-data db)))


(reg-sub
  ::fields-in-errors
  :<- [::form-data]
  (fn [form-data]
    (let [errors [(when-not
                    (= (:new-password form-data)
                       (:repeat-new-password form-data))
                    "password")
                  (when (empty? (:current-password form-data))
                    "error")
                  (when (empty? (:new-password form-data))
                    "error")]]
      (->> errors seq (remove nil?) set))))


(reg-sub
  ::form-error?
  :<- [::fields-in-errors]
  (fn [fields-in-errors]
    (some? (seq fields-in-errors))))