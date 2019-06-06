(ns sixsq.nuvla.ui.edge.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::stale-count nat-int?)

(s/def ::active-count nat-int?)

(s/def ::nuvlaboxes any?)

(s/def ::state-nuvlaboxes any?)

(s/def ::status-nuvlaboxes any?)

(s/def ::page int?)
(s/def ::elements-per-page int?)
(s/def ::total-elements int?)

(s/def ::state-selector #{"all" "new" "activated" "commissioned" "decommissioning" "decommissioned" "error"})


(s/def ::db (s/keys :req [::loading?
                          ::nuvlaboxes
                          ::state-nuvlaboxes
                          ::status-nuvlaboxes
                          ::page
                          ::elements-per-page
                          ::total-elements
                          ::state-selector]))


(def defaults {::loading?          false
               ::nuvlaboxes        nil
               ::state-nuvlaboxes  nil
               ::status-nuvlaboxes nil
               ::page              1
               ::elements-per-page 10
               ::total-elements    0
               ::state-selector    nil})
