(ns sixsq.nuvla.ui.nuvlabox.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::stale-count nat-int?)
(s/def ::active-count nat-int?)
(s/def ::healthy? (s/map-of string? boolean?))
(s/def ::health-info (s/keys :req-un [::stale-count
                                      ::active-count
                                      ::healthy?]))

(s/def ::nuvlaboxes any?)

(s/def ::state-nuvlaboxes any?)

(s/def ::status-nuvlaboxes any?)

(s/def ::page int?)
(s/def ::elements-per-page int?)
(s/def ::total-elements int?)

(s/def ::state-selector #{"all" "new" "activated" "quarantined"})


(s/def ::db (s/keys :req [::loading?
                          ::health-info
                          ::nuvlaboxes
                          ::state-nuvlaboxes
                          ::status-nuvlaboxes
                          ::page
                          ::elements-per-page
                          ::total-elements
                          ::state-selector]))


(def defaults {::loading?          false
               ::health-info       {:stale-count  0
                                    :active-count 0
                                    :healthy?     {}}

               ::nuvlaboxes        nil
               ::state-nuvlaboxes  nil
               ::status-nuvlaboxes nil
               ::page              1
               ::elements-per-page 10
               ::total-elements    0
               ::state-selector    nil})

