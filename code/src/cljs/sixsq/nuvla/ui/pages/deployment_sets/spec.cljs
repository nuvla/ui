(ns sixsq.nuvla.ui.pages.deployment-sets.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.common-components.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]))

(s/def ::deployment-sets any?)
(s/def ::deployment-sets-summary any?)
(s/def ::state-selector (s/nilable string?))

(def defaults
  {::deployment-sets         nil
   ::deployment-sets-summary nil
   ::state-selector          nil
   ::pagination              (pagination-plugin/build-spec)
   ::search                  (full-text-search-plugin/build-spec)})
