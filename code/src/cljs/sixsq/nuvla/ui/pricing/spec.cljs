(ns sixsq.nuvla.ui.pricing.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::stripe any?)

(s/def ::subscription any?)

(s/def ::error string?)

(s/def ::processing? boolean?)

(s/def ::db (s/keys :req [::stripe
                          ::subscription
                          ::error
                          ::processing?]))


(def defaults {::stripe       nil
               ::subscription nil
               ::error        nil
               ::processing?  false})
