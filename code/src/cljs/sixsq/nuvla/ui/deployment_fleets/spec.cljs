(ns sixsq.nuvla.ui.deployment-fleets.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
    [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]))

(s/def ::deployment-fleets any?)
(s/def ::deployment-fleets-summary any?)
(s/def ::state-selector (s/nilable string?))

(def defaults
  {::deployment-fleets         nil
   ::deployment-fleets-summary nil
   ::state-selector            nil
   ::pagination                (pagination-plugin/build-spec)
   ::search                    (full-text-search-plugin/build-spec)})
