(ns sixsq.nuvla.ui.edge.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::stale-count nat-int?)

(s/def ::active-count nat-int?)

(s/def ::nuvlaboxes any?)

(s/def ::nuvlabox-releases any?)

(s/def ::state-nuvlaboxes any?)

(s/def ::status-nuvlaboxes any?)

(s/def ::open-modal (s/nilable keyword?))

(s/def ::nuvlabox-created-id (s/nilable string?))

(s/def ::page int?)
(s/def ::elements-per-page int?)
(s/def ::total-elements int?)

(s/def ::full-text-search (s/nilable string?))

(s/def ::state-selector #{"all" "new" "activated" "commissioned"
                          "decommissioning" "decommissioned" "error"})

(s/def ::vpn-infra any?)


(s/def ::db (s/keys :req [::loading?
                          ::nuvlaboxes
                          ::nuvlabox-releases
                          ::state-nuvlaboxes
                          ::status-nuvlaboxes
                          ::page
                          ::elements-per-page
                          ::total-elements
                          ::full-text-search
                          ::state-selector
                          ::open-modal
                          ::nuvlabox-created-id
                          ::vpn-infra]))


(def defaults {::loading?            false
               ::nuvlaboxes          nil
               ::nuvlabox-releases   nil
               ::state-nuvlaboxes    nil
               ::status-nuvlaboxes   nil
               ::page                1
               ::elements-per-page   9
               ::total-elements      0
               ::full-text-search    nil
               ::state-selector      nil
               ::open-modal          nil
               ::nuvlabox-created-id nil
               ::vpn-infra           nil})
