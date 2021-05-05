(ns sixsq.nuvla.ui.profile.effects
  (:require
    [re-frame.core :refer [reg-fx]]))


(reg-fx
  ::confirm-card-setup
  (fn [[^js stripe client-secret data callback]]
    (-> (. stripe confirmCardSetup client-secret data)
        (.then (fn [result]
                 (callback result))))))


(reg-fx
  ::confirm-sepa-debit-setup
  (fn [[^js stripe client-secret data callback]]
    (-> (. stripe confirmSepaDebitSetup client-secret data)
        (.then (fn [result]
                 (callback result))))))
