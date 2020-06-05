(ns sixsq.nuvla.ui.profile.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::user any?)

(s/def ::customer any?)

(s/def ::subscription any?)

(s/def ::payment-methods any?)

(s/def ::upcoming-invoice any?)

(s/def ::invoices any?)

(s/def ::customer-info any?)

(s/def ::open-modal (s/nilable keyword?))

(s/def ::error-message (s/nilable string?))

(s/def ::loading set?)

(s/def ::plan-id (s/nilable string?))

(s/def ::setup-intent any?)


(s/def ::db (s/keys :req [::user
                          ::customer
                          ::subscription
                          ::payment-methods
                          ::upcoming-invoice
                          ::invoices
                          ::customer-info
                          ::open-modal
                          ::error-message
                          ::loading
                          ::setup-intent]))


(def defaults {::user             nil
               ::customer         nil
               ::payment-methods  nil
               ::upcoming-invoice nil
               ::invoices         nil
               ::customer-info    nil
               ::subscription     nil
               ::setup-intent     nil
               ::open-modal       nil
               ::error-message    nil
               ::loading          #{}})
