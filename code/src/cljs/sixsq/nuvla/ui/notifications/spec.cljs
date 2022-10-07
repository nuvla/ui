(ns sixsq.nuvla.ui.notifications.spec
  (:require
   [clojure.spec.alpha :as s]
   [sixsq.nuvla.ui.utils.spec :as utils-spec]))

(s/def ::name utils-spec/nonblank-string)
(s/def ::description utils-spec/nonblank-string)
(s/def ::method utils-spec/nonblank-string)
(s/def ::type utils-spec/nonblank-string)
(s/def ::category utils-spec/nonblank-string)

;;
;; notification method
;;

(s/def ::notification-methods (s/nilable any?))
(s/def ::add-notification-method-modal-visible? boolean?)
(s/def ::notification-method-modal-visible? boolean?)

(s/def ::destination utils-spec/nonblank-string)
(s/def ::notification-method (s/keys :req-un [::name
                                              ::description
                                              ::method
                                              ::destination]))

;;
;; notification subscription configuration
;;

(s/def ::notification-subscription-configs (s/nilable any?))

(s/def ::resource-tag utils-spec/nonblank-string)
(s/def ::resource-filter string?)
(s/def ::resource-tags-available any?)

(s/def ::kind utils-spec/nonblank-string)
(s/def ::metric utils-spec/nonblank-string)
(s/def ::value utils-spec/nonblank-string)
(s/def ::condition utils-spec/nonblank-string)
(s/def ::window integer?)
(s/def ::value-type utils-spec/nonblank-string)

(def reset-interval-regex #"^(month|[1-9]{1,2}[0-9]d|[1-9]d)$")
(s/def ::reset-interval #(re-matches reset-interval-regex %))

(s/def ::reset-start-date #(and (integer? %)
                                (< 0 %)
                                (< % 32)))

(s/def ::reset-in-days #(and (integer? %)
                                (< 0 %)
                                (< % 1000)))

(s/def ::dev-name string?)

(s/def ::criteria
  (s/keys :req-un [::kind
                   ::metric
                   ::condition]
          :opt-un [::window
                   ::value
                   ::reset-interval
                   ::reset-start-date
                   ::reset-in-days
                   ::dev-name
                   ::value-type]))

(s/def ::method-ids
  (s/and (s/coll-of utils-spec/nonblank-string :distinct true :kind vector?)
         not-empty))
(s/def ::criteria-value utils-spec/nonblank-string)
(s/def ::criteria-condition utils-spec/nonblank-string)

(s/def ::components-number utils-spec/nonblank-string)
(s/def ::collection utils-spec/nonblank-string)
(s/def ::enabled boolean?)
(s/def ::notification-subscription-config-id utils-spec/nonblank-string)


(s/def ::notification-subscription-config
  (s/keys :req-un [::name
                   ::description
                   ::enabled
                   ::category
                   ::collection
                   ::method-ids
                   ::criteria]
          :opt-un [::resource-filter]))
(s/def ::notification-subscriptions-modal-visible? boolean?)
(s/def ::add-subscription-modal-visible? boolean?)
(s/def ::add-subscription-config-modal-visible? boolean?)
(s/def ::edit-subscription-config-modal-visible? boolean?)

(def component-types #{"nuvlabox"
                       "infrastructure-service"
                       "deployment"})

(s/def ::component-type (s/spec component-types))

;;
;; individual subscriptions
;;

(s/def ::resource utils-spec/nonblank-string)
(s/def ::status utils-spec/nonblank-string)

(s/def ::subscriptions (s/nilable any?))
(s/def ::subscriptions-for-parent (s/nilable any?))
(s/def ::subscriptions-by-parent (s/nilable any?))
(s/def ::subscriptions-by-parent-counts (s/nilable any?))
(s/def ::notification-subscriptions (s/nilable any?))

(s/def ::subscription (s/keys :req-un [::type
                                       ::kind
                                       ::category
                                       ::resource
                                       ::status
                                       ::method]
                              :opt-un [::name
                                       ::description]))

(s/def ::edit-subscription-modal-visible? boolean?)

;;
;; Validation
;;

(s/def ::form-valid? boolean?)

(s/def ::form-spec any?)

(s/def ::validate-form? boolean?)

(s/def ::is-new? boolean?)

(s/def ::notification-method-create-button-visible? boolean?)


(def defaults {::notification-method                    {}
               ::notification-methods                   []
               ::add-notification-method-modal-visible? false
               ::notification-method-modal-visible?     false})
