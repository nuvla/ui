(ns sixsq.nuvla.ui.profile.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as us]
    [sixsq.nuvla.ui.plugins.tab :as tab-plugin]))

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
(s/def ::tab any?)
(s/def ::two-factor-step (s/nilable string?))
(s/def ::two-factor-enable? boolean?)
(s/def ::two-factor-method (s/nilable string?))
(s/def ::two-factor-callback (s/nilable string?))
(s/def ::two-factor-secret (s/nilable string?))

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
               ::tab                 (tab-plugin/build-spec)
               ::two-factor-step     :install-app
               ::two-factor-enable?  true
               ::two-factor-method   nil
               ::two-factor-callback nil
               ::two-factor-secret   nil})
