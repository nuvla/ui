(ns sixsq.nuvla.ui.deployment-fleets-detail.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.tab :as tab]
    [sixsq.nuvla.ui.plugins.events-table :as events-table]
    [sixsq.nuvla.ui.plugins.step-group :as step-group]))

(s/def ::deployment-fleet (s/nilable any?))
(s/def ::deployment-fleet-not-found? boolean?)
(s/def ::apps (s/nilable any?))
(s/def ::apps-fulltext-search (s/nilable string?))
(s/def ::apps-selected (s/nilable set?))
(s/def ::apps-loading? boolean?)
(s/def ::creds (s/nilable any?))
(s/def ::creds-fulltext-search (s/nilable string?))

(def defaults
  {::deployment-fleet            nil
   ::deployment-fleet-not-found? false
   ::apps                        nil
   ::apps-fulltext-search        nil
   ::apps-selected               #{}
   ::apps-loading?               false
   ::creds                       nil
   ::creds-fulltext-search       nil
   ::creds-selected              #{}
   ::events                      (events-table/build-spec)
   ::tab                         (tab/build-spec :active-tab :overview)
   ::tab-new-apps                (tab/build-spec :active-tab :my-apps)
   ::tab-new-targets             (tab/build-spec :active-tab :edges)
   ::steps                       (step-group/build-spec
                                   :active-step :select-apps-targets)})
