(ns sixsq.nuvla.ui.deployment-sets-detail.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.plugins.step-group :as step-group-plugin]))

(s/def ::deployment-set (s/nilable any?))

(s/def ::name string?)
(s/def ::description string?)
(s/def ::version string?)
(s/def ::apps (s/coll-of map? :kind vector?))
(s/def ::edges (s/coll-of map? :kind vector?))
(s/def ::deployments (s/coll-of map? :kind vector?))

(s/def ::deployment-set-not-found? boolean?)
(s/def ::targets-selected (s/nilable set?))
(s/def ::bulk-jobs any?)
(s/def ::create-name string?)
(s/def ::create-description string?)
(s/def ::module-applications-sets (s/nilable map?))

(def defaults
  {::module-applications-sets  nil
   ::apps-sets                 nil
   ::deployment-set            nil
   ::deployment-set-not-found? false
   ::events                    (events-plugin/build-spec)
   ::steps                     (step-group-plugin/build-spec
                                 :active-step :name)
   ::targets-selected          #{}
   ::bulk-jobs                 (bulk-progress-plugin/build-spec)
   ::create-name               ""
   ::create-description        ""
   ::licenses-accepted?        false
   ::prices-accepted?          false})
