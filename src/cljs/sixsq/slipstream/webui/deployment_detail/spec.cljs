(ns sixsq.slipstream.webui.deployment-detail.spec
  (:require
    [clojure.spec.alpha :as s]
    [reagent.core :as reagent]))

(s/def ::runUUID (s/nilable string?))                       ; Used by old UI

(s/def ::reports any?)

(s/def ::loading? boolean?)

(s/def ::deployment any?)

(s/def ::global-deployment-parameters any?)

(s/def ::events any?)

(s/def ::jobs any?)

(s/def ::node-parameters-modal (s/nilable string?))

(s/def ::node-parameters any?)

(s/def ::summary-nodes-parameters any?)

(s/def ::force-refresh-events-steps string?)


(s/def ::db (s/keys :req [::runUUID
                          ::reports
                          ::loading?
                          ::deployment
                          ::global-deployment-parameters
                          ::events
                          ::jobs
                          ::node-parameters-modal
                          ::node-parameters
                          ::summary-nodes-parameters
                          ::force-refresh-events-steps]))


(def defaults {::runUUID                      nil
               ::reports                      nil
               ::loading?                     false
               ::deployment                   nil
               ::global-deployment-parameters nil
               ::events                       nil
               ::jobs                         nil
               ::node-parameters-modal        nil
               ::node-parameters              nil
               ::summary-nodes-parameters     nil
               ::force-refresh-events-steps   "force-refresh"})
