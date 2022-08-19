(ns sixsq.nuvla.ui.deployment-fleets.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.pagination :as pagination]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search]))

(s/def ::deployment-fleets any?)
(s/def ::deployment-fleets-summary any?)
(s/def ::full-text-search (s/nilable string?))
(s/def ::state-selector (s/nilable string?))

(def defaults
  {::deployment-fleets         nil
   ::deployment-fleets-summary nil
   ::full-text-search          nil
   ::state-selector            nil
   ::pagination                (pagination/build-spec
                                 :default-items-per-page 1)
   ::search                    (full-text-search/build-spec)})
