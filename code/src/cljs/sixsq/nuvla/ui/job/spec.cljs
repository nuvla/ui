(ns sixsq.nuvla.ui.job.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]))

(s/def ::target-resource string?)
(s/def ::jobs any?)
(s/def ::pagination any?)

(def defaults {::target-resource nil
               ::jobs            nil})

(def pagination-default {::pagination (pagination-plugin/build-spec)})
