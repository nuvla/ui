(ns sixsq.nuvla.ui.profile.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as us]))


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

(s/def ::setup-intent any?)

(s/def ::vendor any?)

(s/def ::group any?)

(s/def ::group-name us/nonblank-string)

(s/def ::group-description us/nonblank-string)

(s/def ::group-form (s/keys :req [::group-name
                                  ::group-description]))

(s/def ::active-tab keyword?)

(s/def ::two-factor-step (s/nilable string?))

(s/def ::two-factor-enable? boolean?)

(s/def ::two-factor-method (s/nilable string?))

(s/def ::two-factor-callback (s/nilable string?))

(s/def ::two-factor-secret (s/nilable string?))

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
                          ::setup-intent
                          ::vendor
                          ::group
                          ::active-tab
                          ::two-factor-step
                          ::two-factor-enable?
                          ::two-factor-method
                          ::two-factor-callback
                          ::two-factor-secret]))


(def defaults {::user                nil
               ::customer            nil
               ::payment-methods     nil
               ::upcoming-invoice    nil
               ::invoices            nil
               ::customer-info       nil
               ::subscription        nil
               ::setup-intent        nil
               ::open-modal          nil
               ::error-message       nil
               ::loading             #{}
               ::vendor              nil
               ::group               nil
               ::active-tab          :subscription
               ::two-factor-step     :install-app
               ::two-factor-enable?  true
               ::two-factor-method   nil
               ::two-factor-callback nil
               ::two-factor-secret   nil})
