(ns sixsq.nuvla.ui.profile.effects
  (:require
    [re-frame.core :refer [dispatch reg-fx]]))


(reg-fx
  ::create-payment-method
  (fn [[stripe data callback]]
    (js/console.error "::create-payment-method " stripe data callback)
    (js/console.error "::create-payment-method " (.-createPaymentMethod stripe))
    (js/console.error "::create-payment-method " (. stripe -createPaymentMethod))
    (-> ((.-createPaymentMethod stripe) data)
        (.then (fn [result] (callback result))))))


(reg-fx
  ::confirm-card-setup
  (fn [[stripe client-secret data callback]]
    (js/console.error "::confirm-card-setup " stripe)
    (-> (. stripe confirmCardSetup client-secret data)
        (.then (fn [result]
                 (callback result))))))


(reg-fx
  ::confirm-sepa-debit-setup
  (fn [[stripe client-secret data callback]]
    (js/console.error "::confirm-sepa-debit-setup" stripe)
    (-> (. stripe confirmSepaDebitSetup client-secret data)
        (.then (fn [result]
                 (callback result))))))
