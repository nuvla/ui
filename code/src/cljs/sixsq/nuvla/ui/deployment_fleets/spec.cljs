(ns sixsq.nuvla.ui.deployment-fleets.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.pagination :as pagination]))

(s/def ::deployment-fleets any?)

(s/def ::deployment-fleets-summary any?)

(s/def ::page int?)

(s/def ::elements-per-page int?)

(s/def ::total-elements int?)

(s/def ::full-text-search (s/nilable string?))

(s/def ::state-selector (s/nilable string?))

(s/def ::db (s/keys :req [::deployment-fleets
                          ::deployment-fleets-summary
                          ::page
                          ::elements-per-page
                          ::total-elements
                          ::full-text-search
                          ::state-selector]))

(def defaults
  {::deployment-fleets         nil
   ::deployment-fleets-summary nil
   ::page                      1
   ::elements-per-page         8
   ::total-elements            0
   ::full-text-search          nil
   ::state-selector            nil
   ::pagination                (pagination/build-spec
                                 :default-items-per-page 1)})
