(ns sixsq.nuvla.ui.profile.effects
  (:require
    ["@stripe/stripe-js" :as stripejs]
    [re-frame.core :refer [dispatch reg-fx]]))


(reg-fx
  ::create-payment-method
  (fn [[stripe data callback]]
    (js/console.error "::create-payment-method " stripe)
    (-> stripe
        (.createPaymentMethod data)
        (.then (fn [result] (callback result))))))


(reg-fx
  ::confirm-card-setup
  (fn [[stripe client-secret data callback]]
    (js/console.error "::confirm-card-setup " stripe)
    (-> stripe
        (.confirmCardSetup client-secret data)
        (.then (fn [result]
                 (callback result))))))


(reg-fx
  ::confirm-sepa-debit-setup
  (fn [[stripe client-secret data callback]]
    (js/console.error "::confirm-sepa-debit-setup" stripe)
    (-> stripe
        (.confirmSepaDebitSetup client-secret data)
        (.then (fn [result]
                 (callback result))))))
