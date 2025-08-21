(ns sixsq.nuvla.ui.pages.profile.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.common-components.plugins.audit-log :as audit-log-plugin]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as nav-tab]))

(s/def ::user any?)
(s/def ::customer any?)
(s/def ::subscription any?)
(s/def ::app-subscriptions any?)
(s/def ::payment-methods any?)
(s/def ::upcoming-invoice any?)
(s/def ::invoices any?)
(s/def ::customer-info any?)
(s/def ::open-modal (s/nilable keyword?))
(s/def ::error-message (s/nilable string?))
(s/def ::loading set?)
(s/def ::setup-intent any?)
(s/def ::vendor any?)
(s/def ::tab any?)
(s/def ::two-factor-step (s/nilable string?))
(s/def ::two-factor-enable? boolean?)
(s/def ::two-factor-method (s/nilable string?))
(s/def ::two-factor-callback (s/nilable string?))
(s/def ::two-factor-secret (s/nilable string?))

(s/def ::events any?)

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
               ::tab                 (nav-tab/build-spec)
               ::two-factor-step     :install-app
               ::two-factor-enable?  true
               ::two-factor-method   nil
               ::two-factor-callback nil
               ::two-factor-secret   nil
               ::events              (audit-log-plugin/build-spec
                                       :default-items-per-page 15)})
