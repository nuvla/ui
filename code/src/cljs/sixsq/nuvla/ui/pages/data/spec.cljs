(ns sixsq.nuvla.ui.pages.data.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.common-components.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as tab-plugin]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]))

(s/def ::data-records any?)
(s/def ::credentials (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::content-type-filter (s/nilable string?))
(s/def ::application-select-visible? boolean?)
(s/def ::loading-applications? boolean?)
(s/def ::applications (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::selected-application-id (s/nilable string?))
(s/def ::data-search any?)
(s/def ::pagination any?)
(s/def ::tab any?)
(s/def ::total any?)
(s/def ::counts any?)
(s/def ::sizes any?)
(s/def ::data-sets (s/map-of string? map?))
(s/def ::selected-data-set-ids (s/coll-of string? :kind set?))
(s/def ::modal-open? boolean?)
(s/def ::add-data-set-form any?)

(def defaults
  {::modal-open?                 false
   ::data-records                nil
   ::credentials                 nil
   ::application-select-visible? false
   ::loading-applications?       false
   ::applications                nil
   ::content-type-filter         nil
   ::data-search                 (full-text-search-plugin/build-spec)
   ::total                       0
   ::counts                      nil
   ::sizes                       nil
   ::data-sets                   {}
   ::selected-data-set-ids       #{}
   ::add-data-set-form           {}
   ::tab                         (tab-plugin/build-spec :default-tab :data-sets)})

(def pagination-default {::pagination (pagination-plugin/build-spec
                                        :default-items-per-page 8)})
