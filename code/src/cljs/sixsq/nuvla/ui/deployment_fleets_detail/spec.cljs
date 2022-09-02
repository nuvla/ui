(ns sixsq.nuvla.ui.deployment-fleets-detail.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.events :as events-plugin]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
    [sixsq.nuvla.ui.plugins.pagination :as pagination]
    [sixsq.nuvla.ui.plugins.step-group :as step-group]
    [sixsq.nuvla.ui.plugins.tab :as tab]))

(s/def ::deployment-fleet (s/nilable any?))
(s/def ::deployment-fleet-not-found? boolean?)
(s/def ::apps (s/nilable any?))
(s/def ::apps-selected (s/nilable set?))
(s/def ::targets-selected (s/nilable set?))
(s/def ::apps-loading? boolean?)
(s/def ::targets-loading? boolean?)
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
   ::credentials                 nil
   ::edges                       nil
   ::infrastructures             nil
   ::events                      (events-plugin/build-spec)
   ::apps-search                 (full-text-search-plugin/build-spec)
   ::creds-search                (full-text-search-plugin/build-spec)
   ::steps                       (step-group/build-spec
                                   :active-step :select-apps-targets)
   ::targets-selected            #{}
   ::edges-search                (full-text-search-plugin/build-spec)
   ::clouds-pagination           (pagination/build-spec
                                   :default-items-per-page 15)
   ::clouds-search               (full-text-search-plugin/build-spec)
   ::apps-pagination             (pagination/build-spec
                                   :default-items-per-page 15)
   ::edges-pagination             (pagination/build-spec
                                   :default-items-per-page 15)
   ::config-apps-tab             (tab/build-spec :active-tab :my-apps)})
