(ns sixsq.nuvla.ui.profile.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as us]))


(s/def ::stripe any?)

(s/def ::user any?)

(s/def ::customer any?)

(s/def ::open-modal (s/nilable keyword?))

(s/def ::error-message (s/nilable string?))

(s/def ::loading set?)

(s/def ::plan-id (s/nilable string?))

(s/def ::processing? boolean?)


(s/def ::db (s/keys :req [::stripe
                          ::user
                          ::customer
                          ::open-modal
                          ::error-message
                          ::processing?
                          ::loading]))


(def defaults {::stripe        nil
               ::user          nil
               ::customer      nil
               ::open-modal    nil
               ::error-message nil
               ::processing?   false
               ::loading       #{:user :customer}})
