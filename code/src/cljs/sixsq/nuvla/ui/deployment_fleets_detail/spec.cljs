(ns sixsq.nuvla.ui.deployment-fleets-detail.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.tab :as tab]
    [sixsq.nuvla.ui.plugins.events-table :as events-table]
    [sixsq.nuvla.ui.plugins.step-group :as step-group]))

(s/def ::deployment-fleet (s/nilable any?))
(s/def ::deployment-fleet-events (s/nilable any?))
(s/def ::elements-per-page int?)
(s/def ::page int?)
(s/def ::deployment-fleet-not-found? boolean?)
(s/def ::apps (s/nilable any?))
(s/def ::apps-fulltext-search (s/nilable string?))
(s/def ::apps-selected (s/nilable set?))
(s/def ::apps-loading? boolean?)
(s/def ::creds (s/nilable any?))
(s/def ::creds-fulltext-search (s/nilable string?))
(s/def ::tab (s/nilable any?))
(s/def ::steps (s/nilable any?))
(s/def ::tab-new-apps (s/nilable any?))
(s/def ::tab-new-targets (s/nilable any?))


(s/def ::db (s/keys :req [::deployment-fleet
                          ::deployment-fleet-events
                          ::elements-per-page
                          ::page
                          ::deployment-fleet-not-found?
                          ::apps
                          ::apps-fulltext-search
                          ::apps-selected
                          ::apps-loading?
                          ::creds
                          ::creds-fulltext-search
                          ::creds-selected
                          ::tab-new-apps
                          ::tab]))

(def defaults
  {::deployment-fleet            nil
   ::deployment-fleet-events     nil
   ::elements-per-page           15
   ::page                        1
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
