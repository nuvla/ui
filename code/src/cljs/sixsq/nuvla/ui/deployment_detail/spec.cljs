(ns sixsq.nuvla.ui.deployment-detail.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::loading? boolean?)

(s/def ::deployment any?)

(s/def ::deployment-parameters any?)

(s/def ::events any?)

(s/def ::jobs any?)

(s/def ::node-parameters any?)


(s/def ::db (s/keys :req [::loading?
                          ::deployment
                          ::deployment-parameters
                          ::events
                          ::jobs
                          ::node-parameters]))


(def defaults {::loading?                   false
               ::deployment                 nil
               ::deployment-parameters      nil
               ::events                     nil
               ::jobs                       nil
               ::node-parameters            nil})
