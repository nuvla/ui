(ns sixsq.nuvla.ui.deployment-sets-detail.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.plugins.events :as events-plugin]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
    [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
    [sixsq.nuvla.ui.plugins.step-group :as step-group-plugin]
    [sixsq.nuvla.ui.plugins.tab :as tab-plugin]
    [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]))

(s/def ::deployment-set (s/nilable any?))
(s/def ::deployment-set-not-found? boolean?)
(s/def ::apps (s/nilable any?))
(s/def ::apps-selected (s/nilable set?))
(s/def ::targets-selected (s/nilable set?))
(s/def ::apps-loading? boolean?)
(s/def ::targets-loading? boolean?)
(s/def ::edges (s/nilable any?))
(s/def ::infrastructures (s/nilable any?))
(s/def ::credentials (s/nilable any?))
(s/def ::bulk-jobs any?)

(def defaults
  {::deployment-set            nil
   ::deployment-set-not-found? false
   ::apps                      nil
   ::apps-selected             #{}
   ::apps-loading?             false
   ::targets-loading?          false
   ::credentials               nil
   ::edges                     nil
   ::infrastructures           nil
   ::events                    (events-plugin/build-spec)
   ::apps-search               (full-text-search-plugin/build-spec)
   ::creds-search              (full-text-search-plugin/build-spec)
   ::steps                     (step-group-plugin/build-spec
                                 :active-step :select-apps-targets)
   ::targets-selected          #{}
   ::edges-search              (full-text-search-plugin/build-spec)
   ::clouds-pagination         (pagination-plugin/build-spec
                                 :default-items-per-page 15)
   ::clouds-search             (full-text-search-plugin/build-spec)
   ::apps-pagination           (pagination-plugin/build-spec
                                 :default-items-per-page 15)
   ::edges-pagination          (pagination-plugin/build-spec
                                 :default-items-per-page 15)
   ::tab-new-apps              (tab-plugin/build-spec :active-tab :app-store)
   ::bulk-jobs                 (bulk-progress-plugin/build-spec)})
