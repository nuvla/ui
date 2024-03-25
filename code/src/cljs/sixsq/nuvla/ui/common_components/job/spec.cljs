(ns sixsq.nuvla.ui.common-components.job.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]))

(s/def ::target-resource string?)
(s/def ::jobs any?)
(s/def ::pagination any?)

(def defaults {::target-resource nil
               ::jobs            nil})

(def pagination-default {::pagination (pagination-plugin/build-spec)})
