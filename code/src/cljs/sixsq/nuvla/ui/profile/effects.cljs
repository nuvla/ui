(ns sixsq.nuvla.ui.profile.effects
  (:require
    [re-frame.core :refer [dispatch reg-fx]]))


(reg-fx
  ::create-payment-method
  (fn [[stripe data callback]]
    (-> ((.-createPaymentMethod stripe) data)
        (.then (fn [result] (callback result))))))


(reg-fx
  ::confirm-card-setup
  (fn [[stripe client-secret data callback]]
    (-> (. stripe confirmCardSetup client-secret data)
        (.then (fn [result]
                 (callback result))))))


(reg-fx
  ::confirm-sepa-debit-setup
  (fn [[stripe client-secret data callback]]
    (-> (. stripe confirmSepaDebitSetup client-secret data)
        (.then (fn [result]
                 (callback result))))))
