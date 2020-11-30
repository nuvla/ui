(ns sixsq.nuvla.ui.notifications.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as utils-spec]))


(s/def ::notification-methods (s/nilable any?))
(s/def ::add-notification-method-modal-visible? boolean?)
(s/def ::notification-method-modal-visible? boolean?)

(s/def ::name utils-spec/nonblank-string)
(s/def ::description utils-spec/nonblank-string)

(s/def ::method utils-spec/nonblank-string)
(s/def ::destination utils-spec/nonblank-string)
(s/def ::notification-method (s/keys :req-un [::name
                                              ::description
                                              ::method
                                              ::destination]))

(s/def ::notification-subscription-configs (s/nilable any?))

(s/def ::type utils-spec/nonblank-string)
(s/def ::collection utils-spec/nonblank-string)
(s/def ::category utils-spec/nonblank-string)
(s/def ::enabled boolean?)
(s/def ::notification-subscription-config (s/keys :req-un [::name
                                                           ::description
                                                           ::type
                                                           ::collection
                                                           ::category
                                                           ::method
                                                           ::enabled]))
(s/def ::notification-subscriptions-modal-visible? boolean?)
(s/def ::notification-subscriptions (s/nilable any?))

;;
;; subscriptions
;;

(s/def ::subscriptions (s/nilable any?))

(s/def ::subscription (s/nilable any?))


;; Validation

(s/def ::form-valid? boolean?)

(s/def ::form-spec any?)

(s/def ::validate-form? boolean?)

(s/def ::is-new? boolean?)

(s/def ::db (s/keys :req [::add-notification-method-modal-visible?
                          ::notification-method-modal-visible?]))


(def defaults {::notification-method                    {}
               ::notification-methods                   []
               ::add-notification-method-modal-visible? false
               ::notification-method-modal-visible?     false})

