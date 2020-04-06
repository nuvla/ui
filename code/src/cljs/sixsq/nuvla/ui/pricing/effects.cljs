(ns sixsq.nuvla.ui.pricing.effects
  (:require
    ["@stripe/stripe-js" :as stripejs]
    [re-frame.core :refer [dispatch reg-fx]]))


(reg-fx
  ::load-stripe
  (fn [[publishable-key callback]]
    (-> publishable-key
        (stripejs/loadStripe)
        (.then (fn [stripe]
                 (callback stripe))))))


(reg-fx
  ::create-payment-method
  (fn [[stripe data callback]]
    (-> stripe
        (.createPaymentMethod data)
        (.then (fn [result] (callback result))))))


(reg-fx
  ::confirm-card-payment
  (fn [[stripe client-secret callback]]
    (-> stripe
        (.confirmCardPayment client-secret)
        (.then (fn [result] (callback result))))))
