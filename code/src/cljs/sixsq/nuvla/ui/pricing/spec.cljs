(ns sixsq.nuvla.ui.pricing.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::stripe any?)

(s/def ::customer any?)

(s/def ::plan-id (s/nilable string?))

(s/def ::error (s/nilable string?))

(s/def ::processing? boolean?)

(s/def ::db (s/keys :req [::stripe
                          ::customer
                          ::error
                          ::processing?]))


(def defaults {::stripe      nil
               ::plan-id     "plan_HGQ9iUgnz2ho8e"          ;;FIXME
               ::customer    nil
               ::error       nil
               ::processing? false})
