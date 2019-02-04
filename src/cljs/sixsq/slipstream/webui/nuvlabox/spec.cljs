(ns sixsq.slipstream.webui.nuvlabox.spec
  (:require-macros [sixsq.slipstream.webui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::stale-count nat-int?)
(s/def ::active-count nat-int?)
(s/def ::healthy? (s/map-of string? boolean?))
(s/def ::health-info (s/keys :req-un [::stale-count
                                      ::active-count
                                      ::healthy?]))

(s/def ::nuvlabox-records any?)

(s/def ::page int?)
(s/def ::elements-per-page int?)
(s/def ::total-elements int?)

(s/def ::state-selector #{"all" "new" "activated" "quarantined"})


(s/def ::db (s/keys :req [::loading?
                          ::health-info
                          ::nuvlabox-records
                          ::page
                          ::elements-per-page
                          ::total-elements
                          ::state-selector]))


(def defaults {::loading?          false
               ::health-info       {:stale-count  0
                                    :active-count 0
                                    :healthy?     {}}

               ::nuvlabox-records  nil
               ::page              1
               ::elements-per-page 10
               ::total-elements    0
               ::state-selector    "all"})

