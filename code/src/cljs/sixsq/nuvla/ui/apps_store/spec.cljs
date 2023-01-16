(ns sixsq.nuvla.ui.apps-store.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.tab :as tab-plugin]))

(s/def ::modules any?)
(s/def ::tab any?)
(s/def ::pagination any?)
(s/def ::modules-search any?)

(def defaults
  {::modules        nil
   ::tab            (tab-plugin/build-spec :active-tab :appstore)
   ::modules-search (full-text-search-plugin/build-spec)})

(def pagination-default {::pagination (pagination-plugin/build-spec
                                        :default-items-per-page 8)})
