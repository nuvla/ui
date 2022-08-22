(ns sixsq.nuvla.ui.deployment-fleets-detail.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.tab :as tab]
    [sixsq.nuvla.ui.plugins.events-table :as events-table]
    [sixsq.nuvla.ui.plugins.step-group :as step-group]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search]
    [sixsq.nuvla.ui.plugins.pagination :as pagination]))

(s/def ::deployment-fleet (s/nilable any?))
(s/def ::deployment-fleet-not-found? boolean?)
(s/def ::apps (s/nilable any?))
(s/def ::apps-selected (s/nilable set?))
(s/def ::apps-loading? boolean?)
(s/def ::targets-loading? boolean?)
(s/def ::creds (s/nilable any?))
(s/def ::edges (s/nilable any?))
(s/def ::infrastructures (s/nilable any?))
(s/def ::credentials (s/nilable any?))

(def defaults
  {::deployment-fleet            nil
   ::deployment-fleet-not-found? false
   ::apps                        nil
   ::apps-selected               #{}
   ::apps-loading?               false
   ::targets-loading?            false
   ::creds                       nil
   ::credentials                 nil
   ::edges                       nil
   ::infrastructures             nil
   ::apps-search                 (full-text-search/build-spec)
   ::creds-search                (full-text-search/build-spec)
   ::creds-selected              #{}
   ::events                      (events-table/build-spec)
   ::tab                         (tab/build-spec :active-tab :overview)
   ::tab-new-apps                (tab/build-spec :active-tab :my-apps)
   ::tab-new-targets             (tab/build-spec :active-tab :edges)
   ::steps                       (step-group/build-spec
                                   :active-step :select-apps-targets)
   ::edges-pagination            (pagination/build-spec
                                   :default-items-per-page 15)
   ::edges-search                (full-text-search/build-spec)})
