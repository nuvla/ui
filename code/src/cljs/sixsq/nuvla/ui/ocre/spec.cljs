(ns sixsq.nuvla.ui.ocre.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::distributor-terms any?)

(s/def ::global-aggregations any?)

(s/def ::platforms-radar any?)

(s/def ::db (s/keys :req [::global-aggregations
                          ::distributor-terms
                          ::platforms-radar]))


(def defaults {::global-aggregations nil
               ::distributor-terms   nil
               ::platforms-radar     nil})
