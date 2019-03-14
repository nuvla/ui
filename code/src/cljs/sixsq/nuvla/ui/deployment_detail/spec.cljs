(ns sixsq.nuvla.ui.deployment-detail.spec
  (:require
    [clojure.spec.alpha :as s]
    [reagent.core :as reagent]))

(s/def ::runUUID (s/nilable string?))                       ; Used by old UI

(s/def ::loading? boolean?)

(s/def ::deployment any?)

(s/def ::deployment-parameters any?)

(s/def ::events any?)

(s/def ::jobs any?)

(s/def ::node-parameters-modal (s/nilable string?))

(s/def ::node-parameters any?)

(s/def ::summary-nodes-parameters any?)

(s/def ::force-refresh-events-steps string?)


(s/def ::db (s/keys :req [::runUUID
                          ::loading?
                          ::deployment
                          ::deployment-parameters
                          ::events
                          ::jobs
                          ::node-parameters-modal
                          ::node-parameters
                          ::summary-nodes-parameters
                          ::force-refresh-events-steps]))


(def defaults {::runUUID                    nil
               ::loading?                   false
               ::deployment                 nil
               ::deployment-parameters      nil
               ::events                     nil
               ::jobs                       nil
               ::node-parameters-modal      nil
               ::node-parameters            nil
               ::summary-nodes-parameters   nil
               ::force-refresh-events-steps "force-refresh"})
