(ns sixsq.nuvla.ui.deployment-sets-detail.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.step-group :as step-group-plugin]))

(s/def ::deployment-set (s/nilable any?))
(s/def ::deployment-set-edited (s/nilable any?))

(s/def ::edges (s/coll-of map? :kind vector?))
(s/def ::edges-documents (s/coll-of map? :kind vector?))
(s/def ::edges-full-text-search (s/nilable string?))
(s/def ::edges-ordering (s/coll-of (s/cat :field keyword? :order #{"desc" "asc" :desc :asc})))
(s/def ::edges-additional-filter string?)
(s/def ::edges-state-selector string?)
(s/def ::edges-pagination any?)

(s/def ::fleet-filter string?)
(s/def ::fleet-filter-edited string?)

(s/def ::validate-form? boolean?)

(def default-ordering [[:created :desc]])
(def pagination-default (pagination-plugin/build-spec
                          :default-items-per-page 25))

(s/def ::deployment-set-not-found? boolean?)
(s/def ::targets-selected (s/nilable set?))
(s/def ::bulk-jobs any?)
(s/def ::create-name string?)
(s/def ::create-description string?)
(s/def ::module-applications-sets (s/nilable map?))

(s/def ::pagination-deployments any?)
(s/def ::edges-pagination any?)
(s/def ::pagination-apps-picker any?)

(s/def ::deployments-summary-all any?)

(s/def ::opened-modal (s/nilable keyword?))

(s/def ::apps-edited? boolean?)

(s/def ::edge-picker-edges any?)
(s/def ::edge-picker-full-text-search (s/nilable string?))
(s/def ::edge-picker-ordering (s/coll-of (s/cat :field keyword? :order #{"desc" "asc" :desc :asc})))
(s/def ::edge-picker-additional-filter string?)
(s/def ::edge-picker-state-selector string?)
(s/def ::edge-picker-pagination any?)
(s/def ::edge-picker-edges-summary any?)

(s/def ::edges-select (s/nilable any?))

(s/def ::edge-picker-select (s/nilable any?))

(def defaults
  {::module-applications-sets  nil
   ::apps-sets                 nil
   ::deployment-set            nil
   ::deployment-set-edited     nil
   ::edges                     nil
   ::edges-documents           nil
   ::validate-form?            false
   ::deployments-summary-all   nil
   ::deployment-set-not-found? false
   ::steps                     (step-group-plugin/build-spec
                                 :active-step :name)
   ::targets-selected          #{}
   ::bulk-jobs                 (bulk-progress-plugin/build-spec)
   ::create-name               ""
   ::create-description        ""
   ::licenses-accepted?        false
   ::prices-accepted?          false
   ::ordering                  default-ordering
   ::pagination-deployments    pagination-default
   ::edges-pagination          pagination-default
   ::pagination-apps-picker    (pagination-plugin/build-spec
                                 :default-items-per-page 16)
   ::edge-picker-pagination   pagination-default
   ::opened-modal              nil
   ::apps-edited?              false
   ::fleet-filter              nil
   ::fleet-filter-edited       nil})

